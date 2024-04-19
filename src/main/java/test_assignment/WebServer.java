package test_assignment;

import lombok.extern.slf4j.Slf4j;
import rawhttp.core.server.Router;
import rawhttp.core.server.TcpRawHttpServer;
import test_assignment.exceptions.ServerConfigurationException;

import java.io.*;
import java.net.ServerSocket;
import java.util.Properties;

@Slf4j
public final class WebServer extends TcpRawHttpServer {
    public WebServer() {
        super(new WebServerOptions());
    }

    @Override
    public void start(Router router) {
        log.info("Server is running");
        super.start(router);
    }

    private static class WebServerOptions implements TcpRawHttpServerOptions {
        @Override
        public ServerSocket getServerSocket() throws ServerConfigurationException {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try(final InputStream is = loader.getResourceAsStream("app.properties")) {
                final Properties props = new Properties();
                props.load(is);
                return new ServerSocket(getPort(props), getBacklog(props));
            } catch (IOException |  NumberFormatException e) {
                throw new ServerConfigurationException("Unable to configure server", e);
            }
        }

        private int getPort(final Properties props) throws NumberFormatException {
            return Integer.parseInt(props.getProperty("server.port"));
        }

        private int getBacklog(final Properties props) throws NumberFormatException {
            return Integer.parseInt(props.getProperty("server.backlog"));
        }
    }
}
