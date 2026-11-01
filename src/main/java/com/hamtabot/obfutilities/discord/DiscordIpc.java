package com.hamtabot.obfutilities.discord;

import java.io.Closeable;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Local connection to the Discord app: a named pipe on Windows, a Unix socket elsewhere. */
final class DiscordIpc implements Closeable {

    static final int OP_HANDSHAKE = 0, OP_FRAME = 1;

    private static final int OP_CLOSE = 2, HEADER_SIZE = 8, MAX_BODY = 64 * 1024, MAX_PIPES = 10;

    private final Closeable handle;
    private final InputStream in;
    private final OutputStream out;

    private DiscordIpc(Closeable handle, InputStream in, OutputStream out) {
        this.handle = handle;
        this.in = in;
        this.out = out;
    }

    /** Null when Discord isn't running. */
    static DiscordIpc open() {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        for (int i = 0; i < MAX_PIPES; i++) {
            try {
                if (windows) return openWindows(i);
            } catch (IOException ignored) {
                continue;
            }
            for (Path dir : unixDirs()) {
                Path socket = dir.resolve("discord-ipc-" + i);
                if (!Files.exists(socket)) continue;
                try {
                    return openUnix(socket);
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    private static DiscordIpc openWindows(int index) throws IOException {
        RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + index, "rw");
        return new DiscordIpc(pipe, new FileInputStream(pipe.getFD()), new FileOutputStream(pipe.getFD()));
    }

    private static DiscordIpc openUnix(Path socket) throws IOException {
        SocketChannel channel = SocketChannel.open(UnixDomainSocketAddress.of(socket));
        return new DiscordIpc(channel, Channels.newInputStream(channel), Channels.newOutputStream(channel));
    }

    private static List<Path> unixDirs() {
        List<Path> dirs = new ArrayList<>();
        for (String env : new String[]{ "XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP" }) {
            String value = System.getenv(env);
            if (value == null || value.isBlank()) continue;
            Path base = Path.of(value);
            dirs.add(base);
            // sandboxed Linux installs of Discord
            dirs.add(base.resolve("app/com.discordapp.Discord"));
            dirs.add(base.resolve("snap.discord"));
        }
        dirs.add(Path.of("/tmp"));
        return dirs;
    }

    /** Discord answers every frame sent here with exactly one frame, so no reader thread is needed. */
    String exchange(int op, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        out.write(ByteBuffer.allocate(HEADER_SIZE + body.length).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(op).putInt(body.length).put(body).array());
        out.flush();

        ByteBuffer header = ByteBuffer.wrap(readFully(HEADER_SIZE)).order(ByteOrder.LITTLE_ENDIAN);
        int replyOp = header.getInt(), length = header.getInt();
        if (length < 0 || length > MAX_BODY) throw new IOException("Invalid Discord frame: " + length + " bytes");
        String reply = new String(readFully(length), StandardCharsets.UTF_8);
        if (replyOp == OP_CLOSE) throw new IOException("Discord closed the connection: " + reply);
        return reply;
    }

    private byte[] readFully(int length) throws IOException {
        byte[] data = in.readNBytes(length);
        if (data.length < length) throw new EOFException("Discord connection lost");
        return data;
    }

    @Override
    public void close() throws IOException { handle.close(); }
}
