import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.StandardOpenOption;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        File currentDirectory = new File(System.getProperty("user.dir"));


        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();


            if (input.isBlank()) {
                continue;
            }

            // Split the input into command and arguments
            String[] parts = parseArguments(input);


            int redirectIndex = -1;
            String redirectOperator = null;

            for (int i = 0; i < parts.length; i++) {

                if (parts[i].equals(">") || parts[i].equals("1>") || parts[i].equals("2>") || parts[i].equals(">>") || parts[i].equals("1>>")|| parts[i].equals("2>>")) {
                    redirectIndex = i;
                    redirectOperator = parts[i];
                    break;
                }
            }

            File stdoutFile = null;
            File stderrFile = null;
            boolean appendStdout = false;
            boolean appendStderr = false;


            if (redirectIndex != -1) {
                File redirectFile = new File(parts[redirectIndex + 1]);

                if (!redirectFile.isAbsolute()) {
                    redirectFile = new File(currentDirectory, parts[redirectIndex + 1]);
                }

                if (redirectOperator.equals(">") || redirectOperator.equals("1>")) {
                    stdoutFile = redirectFile;

                } else if (redirectOperator.equals("2>")) {
                    stderrFile = redirectFile;

                }  else if (redirectOperator.equals(">>") || redirectOperator.equals("1>>")) {
                    appendStdout = true;
                    stdoutFile = redirectFile;

                } else if (redirectOperator.equals("2>>")) {
                stderrFile = redirectFile;
                appendStderr = true;
            }


            }

            String[] commandParts = parts;

            if (redirectIndex != -1) {
                commandParts = new String[redirectIndex];

                for (int i = 0; i < redirectIndex; i++) {
                    commandParts[i] = parts[i];
                }
            }

            String command = commandParts[0];

            if (command.equals("exit") || command.equals("0")) {
                break;

            } else if (command.equals("echo")) {
                StringBuilder output = new StringBuilder();
                for (int i = 1; i < commandParts.length; i++) {
                    if (i > 1) {
                        output.append(" ");
                    }

                    output.append(commandParts[i]);
                }

                if (stdoutFile != null) {
                    if (appendStdout) {
                        Files.writeString(
                                stdoutFile.toPath(),
                                output.toString() + System.lineSeparator(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND
                        );
                    } else {
                        Files.writeString(
                                stdoutFile.toPath(),
                                output.toString() + System.lineSeparator()
                        );
                    }
                } else {
                    System.out.println(output);
                }

                if (stderrFile != null) {
                    if (appendStderr) {
                        Files.writeString(
                                stderrFile.toPath(),
                                "",
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND
                        );
                    } else {
                        Files.writeString(stderrFile.toPath(), "");
                    }
                }

            } else if (command.equals("type")) {
                String commandType = commandParts[1];
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
                String targetPath = commandParts[1];
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
                    runExternalCommand(commandParts, currentDirectory, stdoutFile, stderrFile, appendStdout, appendStderr);
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
    private static void runExternalCommand(
            String[] parts,
            File currentDirectory,
            File stdoutFile,
            File stderrFile,
            boolean appendStdout,
            boolean appendStderr
    ) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(parts);
            processBuilder.directory(currentDirectory);

            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);

            if (stdoutFile != null) {
                if (appendStdout) {
                    processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(stdoutFile));
                } else {
                    processBuilder.redirectOutput(stdoutFile);
                }
            } else {
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }

            if (stderrFile != null) {
                if (appendStderr) {
                    processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(stderrFile));
                } else {
                    processBuilder.redirectError(stderrFile);
                }
            } else {
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
            }

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

            if (currentChar == '\\' && !insideSingleQuotes && !insideDoubleQuotes) {
                if (i + 1 < input.length()) {
                    currentArgument.append(input.charAt(++i));
                }

            } else if (currentChar == '\'' && !insideDoubleQuotes) {
                insideSingleQuotes = !insideSingleQuotes;

            } else if (currentChar == '"' && !insideSingleQuotes) {

                insideDoubleQuotes = !insideDoubleQuotes;

            } else if (Character.isWhitespace(currentChar) && !insideSingleQuotes && !insideDoubleQuotes) {
                if (currentArgument.length() > 0) {
                    arguments.add(currentArgument.toString());
                    currentArgument.setLength(0);
                }

            } else if (currentChar == '\\' && insideDoubleQuotes) {
                if (i + 1 < input.length()) {
                    char nextChar = input.charAt(i + 1);

                    if (nextChar == '"' || nextChar == '\\') {
                        currentArgument.append(nextChar);
                        i++;
                    } else {
                        currentArgument.append(currentChar);
                    }
                } else {
                    currentArgument.append(currentChar);
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