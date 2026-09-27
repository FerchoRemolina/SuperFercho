package com.superfercho.identity.infrastructure.mail;

import com.superfercho.identity.application.port.EmailMessage;
import com.superfercho.identity.application.port.EmailSender;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local/development email adapter. Logs the recovery link to the application console.
 * Must not be selected as the production provider.
 */
public final class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);
    private static final Pattern URL_LINE = Pattern.compile("(?m)^(https?://\\S+)$");

    @Override
    public void send(EmailMessage message) {
        String recoveryUrl = extractUrl(message.textBody()).orElse("(url not found in body)");
        log.info(
                """

                [Password Recovery]
                Email: {}
                Recovery URL:
                {}
                Expires: 15 minutes
                """,
                message.to(),
                recoveryUrl);
    }

    private static java.util.Optional<String> extractUrl(String body) {
        Matcher matcher = URL_LINE.matcher(body);
        if (matcher.find()) {
            return java.util.Optional.of(matcher.group(1));
        }
        return java.util.Optional.empty();
    }
}
