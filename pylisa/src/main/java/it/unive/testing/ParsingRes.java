package it.unive.testing;

public class ParsingRes {
	public String fileName;
	public String status;
	public String error;
	public String stackTrace;

	public ParsingRes(
			String fileName,
			String status,
			String error,
			String stackTrace) {
		this.fileName = fileName;
		this.status = status;
		this.error = error;
		this.stackTrace = stackTrace;
	}
}