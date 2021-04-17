package net.sf.l2j.commons.lang.reference;

public interface HardReference<T>
{
	public T get();
	
	public void clear();
}