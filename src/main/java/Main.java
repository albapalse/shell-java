import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.print("$ ");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            }

            System.out.println(input + ": command not found");
            System.out.print("$ ");

        }
    }
}
