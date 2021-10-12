public class Hello {
    static interface InnerInterface {
        public String get();
    }
    
    public static void main(String[] args) {
        System.out.println("Hello World using " + new InnerInterface() {
            @Override
            public String get() {
                return "anonymous";
            }
        }.get() + "!");
    }
}
