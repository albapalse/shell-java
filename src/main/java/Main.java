import java.io.File;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            // 1. Show the shell prompt
            System.out.print("$ ");

            // 2. Read the full line written by the user
            String input = scanner.nextLine();

            // 3. If the user writes an empty line, start again
            if (input.isBlank()) {
                continue;
            }

            // 4. Split the input into command and arguments
            // Example: "custom_exe alice" -> ["custom_exe", "alice"]
            String[] parts = input.split("\\s+");

            // 5. The command is always the first word
            String command = parts[0];

            // 6. Builtin: exit
            if (input.equals("exit") || input.equals("exit 0")) {
                break;

                // 7. Builtin: echo
            } else if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));

                // 8. Builtin: type
            } else if (input.startsWith("type ")) {
                String commandType = input.substring(5);

                if (isBuiltin(commandType)) {
                    System.out.println(commandType + " is a shell builtin");
                } else {
                    File executable = findExecutable(commandType);

                    if (executable != null) {
                        System.out.println(commandType + " is " + executable.getPath());
                    } else {
                        System.out.println(commandType + ": not found");
                    }
                }

                // 9. External command
            } else {
                File executable = findExecutable(command);

                if (executable != null) {
                    runExternalCommand(parts);
                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }

    // Checks if a command is implemented inside our shell
    private static boolean isBuiltin(String command) {
        return command.equals("echo") || command.equals("exit") || command.equals("type");
    }

    // Searches for a command in PATH
    private static File findExecutable(String command) {
        String path = System.getenv("PATH");

        if (path == null) {
            return null;
        }

        String[] directories = path.split(File.pathSeparator);

        for (String directory : directories) {
            File file = new File(directory, command);

            if (file.exists() && file.canExecute()) {
                return file;
            }
        }

        return null;
    }

    // Runs an external program with its arguments
    private static void runExternalCommand(String[] parts) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(parts);
        processBuilder.inheritIO();

        Process process = processBuilder.start();
        process.waitFor();
    }
}