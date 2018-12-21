package alma.obops.draws.examples.common;

import alma.obops.draws.messages.AbstractMessage;

public class Person extends AbstractMessage {
	
	public String name;
	public int age;
	public boolean alive;

	public Person() {
	}
	
	public Person( String name, int age, boolean alive ) {
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