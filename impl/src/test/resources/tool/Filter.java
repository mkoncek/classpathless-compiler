package java.util.logging;

interface Filter {
	boolean isLoggable​(LogRecord record);
}
