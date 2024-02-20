import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

class Main
{
	public static void main(String[] args)
	{
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		System.out.println(compiler);
	}
}
