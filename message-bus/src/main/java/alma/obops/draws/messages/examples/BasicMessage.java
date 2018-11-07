package alma.obops.draws.messages.examples;

import alma.obops.draws.messages.AbstractMessage;

public class BasicMessage extends AbstractMessage {
	
	public String name;
	public int age;
	public boolean alive;

	public BasicMessage() {
	}
	
	public BasicMessage( String name, int age, boolean alive ) {
		this.age = age;
		this.alive = alive;
		this.name = name;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ "[name=" + name + ", age=" + age + ", alive=" + alive + "]";
	}
}