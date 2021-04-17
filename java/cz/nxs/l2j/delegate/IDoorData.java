/**
 * 
 */
package cz.nxs.l2j.delegate;


/**
 * @author hNoke
 *
 */
public interface IDoorData
{
	public int getDoorId();
	
	public boolean isOpened();
	public void openMe();
	public void closeMe();
}
