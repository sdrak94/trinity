package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import org.strixplatform.StrixPlatform;
import org.strixplatform.managers.ClientGameSessionManager;
import org.strixplatform.managers.ClientProtocolDataManager;
import org.strixplatform.utils.StrixClientData;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.serverpackets.KeyPacket;

/**
 * This class ...
 *
 * @version $Revision: 1.5.2.8.2.8 $ $Date: 2005/04/02 10:43:04 $
 */
public final class ProtocolVersion extends L2GameClientPacket
{
	private static final String _C__00_PROTOCOLVERSION = "[C] 00 ProtocolVersion";
	static Logger _log = Logger.getLogger(ProtocolVersion.class.getName());

    private int _version;
    private byte[] data;
    private int dataChecksum;

	@Override
	protected void readImpl()
	{
		_version  = readD();
        if(StrixPlatform.getInstance().isPlatformEnabled())
        {
            try
            {
                if(_buf.remaining() >= StrixPlatform.getInstance().getProtocolVersionDataSize())
                {
                    data = new byte[StrixPlatform.getInstance().getClientDataSize()];
                    readB(data);
                    dataChecksum = readD();
                }
            }
            catch(final Exception e)
            {
                _log.warning("Client [IP=" + getClient().toString() + "] used unprotected client. Disconnect...");
                getClient().close(new KeyPacket(getClient().enableCrypt(), 0));
                return;
            }
        }
	}

	@Override
	protected void runImpl()
	{
		// this packet is never encrypted
		if (_version == -2)
		{
            if (Config.DEBUG) _log.info("Ping received");
			// this is just a ping attempt from the new C2 client
            getClient().closeNow();
		}
        else if (_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION)
        {
        	if (Config.DEBUG)
        	{
	            _log.info("Client: "+getClient().toString()+" -> Protocol Revision: " + _version + " is invalid. Minimum is "+Config.MIN_PROTOCOL_REVISION+" and Maximum is "+Config.MAX_PROTOCOL_REVISION+" are supported. Closing connection.");
	            _log.warning("Wrong Protocol Version "+_version);
        	}
        	
            KeyPacket pk = new KeyPacket(getClient().enableCrypt(),0);
        	getClient().sendPacket(pk);
        	getClient().setRevision(_version);
        	getClient().setProtocolOk(false);
        }
		if(!StrixPlatform.getInstance().isPlatformEnabled())
        {
        	if (Config.DEBUG)
        	{
        		_log.fine("Client Protocol Revision is ok: "+_version);
        	}

        	KeyPacket pk = new KeyPacket(getClient().enableCrypt(),1);
        	getClient().sendPacket(pk);
        	getClient().setProtocolOk(true);
        }
        else
        {
            if(data == null)
            {
                _log.warning("Client [IP=" + getClient().toString() + "] used unprotected client. Disconnect...");
                getClient().close(new KeyPacket(getClient().enableCrypt(), 0));
                return;
            }
            else
            {
                final StrixClientData clientData = ClientProtocolDataManager.getInstance().getDecodedData(data, dataChecksum);
                if(clientData != null)
                {
                    if(!ClientGameSessionManager.getInstance().checkServerResponse(clientData))
                    {
                        //getClient().close(new KeyPacket(null, clientData));
                        getClient().closeNow();
                        return;
                    }
                    getClient().setStrixClientData(clientData);
                    getClient().setRevision(_version);
                	KeyPacket pk = new KeyPacket(getClient().enableCrypt(),1);
                	getClient().sendPacket(pk);
                	getClient().setProtocolOk(true);
                    return;
                }
                _log.warning("Decode client data failed. See Strix-Platform log file. Disconected client " + getClient().toString());
                getClient().close(new KeyPacket(getClient().enableCrypt(), 0));
            }
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__00_PROTOCOLVERSION;
	}
}
