package de.julianweinelt.databench.worker.flow;

import de.julianweinelt.databench.worker.flow.auth.UserManager;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class FlowCLI {
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public void start() {
        enabled.set(true);
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            while (enabled.get()) {
                String input = reader.readLine("> ");
                if (input == null) break;

                String[] params = input.split(" ");
                String command = params[0].toLowerCase();
                String[] args = new String[params.length - 1];
                System.arraycopy(params, 1, args, 0, args.length);
                if (command.equals("exit")) {
                    //TODO: Exit application
                } else if (command.equals("add-user")) {
                    if (args.length == 2) {
                        String username = args[0];
                        String password = args[1];
                        UserManager.instance().createUser(username, password);
                        log.info("User {} created", username);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
