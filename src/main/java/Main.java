import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.print("$ ");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            } else if (input.startsWith("echo ")) {
            System.out.println(input.substring(5));
            System.out.print("$ ");

            } else if (input.startsWith("type ")) {

                String commandType = input.substring(5);
                if (commandType.equals("echo ")|| commandType.equals("type ")|| commandType.equals("exit ")) {
                    System.out.println(commandType + "is a shell builtin");
                    System.out.print("$ ");

                } else {
                    System.out.println(commandType + ": not found");
                    System.out.print("$ ");

                }

            }

            }
    }
}
