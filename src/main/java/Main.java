import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        File currentDirectory = new File(System.getProperty("user.dir"));


        while (true) {
            // Show the shell prompt
            System.out.print("$ ");

            // Read the full line written by the user
            String input = scanner.nextLine();

            // If the user writes an empty line, start again
            if (input.isBlank()) {
                continue;
            }

            // Split the input into command and arguments
            String[] parts = parseArguments(input);

            String command = parts[0];

            // Builtin: exit
            if (command.equals("exit") || command.equals("0")) {
                break;

                //Builtin: echo
            } else if (command.equals("echo")) {
                StringBuilder output = new StringBuilder();

                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) {
                        output.append(" ");
                    }

                    output.append(parts[i]);
                }

                System.out.println(output);

                // Builtin: type
            } else if (command.equals("type")) {
                String commandType = parts[1];

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

            } else if (command.equals("pwd")) {
                System.out.println(currentDirectory.getAbsolutePath());

            } else if (command.equals("cd")) {
                String targetPath = parts[1];

                if (targetPath.equals("~")) {
                    targetPath = System.getenv("HOME");
                }

                File targetDirectory = new File(targetPath);

                if (!targetDirectory.isAbsolute()) {
                    targetDirectory = new File(currentDirectory, targetPath);
                }

                targetDirectory = targetDirectory.getCanonicalFile();

                if (targetDirectory.exists() && targetDirectory.isDirectory()) {
                    currentDirectory = targetDirectory;
                } else {
                    System.out.println("cd: " + targetPath + ": No such file or directory");
                }

            } else {    // External command
                File executable = findExecutable(command);

                if (executable != null) {
                    runExternalCommand(parts, currentDirectory);
                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }

    // Checks if a command is implemented inside our shell
    private static boolean isBuiltin(String command) {
        return command.equals("echo") || command.equals("exit") || command.equals("type")|| command.equals("pwd") || command.equals("cd");
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
    private static void runExternalCommand(String[] parts, File currentDirectory) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(parts);
            processBuilder.directory(currentDirectory);
            processBuilder.inheritIO();

            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error running command: " + e.getMessage());
        }
    }


    private static String[] parseArguments(String input) {
        List<String> arguments = new ArrayList<>();
        StringBuilder currentArgument = new StringBuilder();

        boolean insideSingleQuotes = false;
        boolean insideDoubleQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);

            if (currentChar == '\'' && !insideDoubleQuotes) {
                insideSingleQuotes = !insideSingleQuotes;

            } else if (currentChar == '"' && !insideSingleQuotes) {

                insideDoubleQuotes = !insideDoubleQuotes;

            } else if (Character.isWhitespace(currentChar) && !insideSingleQuotes && !insideDoubleQuotes) {
                if (currentArgument.length() > 0) {
                    arguments.add(currentArgument.toString());
                    currentArgument.setLength(0);
                }

            } else {
                currentArgument.append(currentChar);
            }
        }

        if (currentArgument.length() > 0) {
            arguments.add(currentArgument.toString());
        }

        return arguments.toArray(new String[0]);
    }
}