package it.unive.ros.models.rclpy;

public class Publisher extends TopicUser {
	public Publisher(Node node, Topic topic, String msgType) {
		super(node, topic, msgType);
	}
}