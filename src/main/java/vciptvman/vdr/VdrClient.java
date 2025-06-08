package vciptvman.vdr;

import vciptvman.model.VdrChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VdrClient {
    private String host;
    private int port;

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public VdrClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public List<VdrChannel> getChannels() throws IOException {
        Pattern channelPattern = Pattern.compile("^.*? (.*?) (.*?)[;,:]{1}.*$");
        String line;
        boolean readMore = true;

        List<VdrChannel> channels = new ArrayList<>();

        connect();

        out.println("lstc :ids");

        while (readMore) {
            line = in.readLine();

            if (line.startsWith("250-")) {
                Matcher matcher = channelPattern.matcher(line);

                if (matcher.matches()) {
                    channels.add(new VdrChannel(matcher.group(1), matcher.group(2), null, null, null, null, 1));
                }
            } else if (line.startsWith("250 ")) {
                readMore = false;
            } else {
                System.err.println("Unknown response: " + line);
                readMore = false;
            }
        }

        disconnect();

        return channels;
    }

    private void connect() throws IOException {
        clientSocket = new Socket(host, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // ignore greeting
        in.readLine();
    }

    private void disconnect() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }
}
