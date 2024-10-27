package org.github.waltz4line.server.javalin.error;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import java.io.IOException;
import java.io.Writer;

public class JettyErrorHandler extends ErrorHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setHeader("Server", "");
        super.handle(target, baseRequest, request, response);
    }

    @Override
    protected void handleErrorPage(HttpServletRequest request, Writer writer, int code, String message) throws IOException {
        writer.write("<html>\n<head>\n");
        super.writeErrorPageHead(request, writer, code, message);
        writer.write("</head>\n<body><h1>");
        message = message == null ? HttpStatus.getMessage(code) : message;
        writer.write(String.valueOf(code));
        writer.write("</h1>\n<pre>");
        writer.write(message);
        writer.write("\n</pre>\n</body>\n</html>\n");
    }

}
