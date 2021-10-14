public class Hello {
    static class Inner {
        String content = "inner";
    }
    
    public static void main(String[] args) {
        var inner = new Inner();
        System.out.println("Hello World using " + inner.content + "!");
    }
}
