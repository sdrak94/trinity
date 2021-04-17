package luna.custom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;

public final class DatabaseBackupManager
{
	//private static final Logger a = LoggerFactory.getLogger(DatabaseBackupManager.class);

	private static Logger a = Logger.getLogger(Announcements.class.getName());
	public static void makeBackup()
	{
		Process process;
		a.info("Initializing Backup Manager.");
		File file;
		if (!(file = new File("./backup/database/")).mkdirs() && !file.exists())
		{
			a.info("Could not create folder " + file.getAbsolutePath());
			return;
		}
		try
		{
			process = Runtime.getRuntime().exec("C://Program Files//MariaDB 10.4//bin --user=" + Config.DATABASE_LOGIN + " --password=" + Config.DATABASE_PASSWORD + " --compact --complete-insert --default-character-set=utf8 --extended-insert --lock-tables --quick --skip-triggers " + "l2trinityBackup", null);
		}
		catch (Exception exception)
		{
			a.warning(DatabaseBackupManager.class.getSimpleName() + ": Could not make backup: " + exception.getMessage());
			return;
		}
		try
		{
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			Date date = new Date();
			if (!(file = new File(file, simpleDateFormat.format(date) + (".sql"))).createNewFile())
				throw new IOException("Cannot create backup file: " + file.getCanonicalPath());
			InputStream inputStream = process.getInputStream();
			Object object = new FileOutputStream(file);
//			if (BackupManagerConfigs.DATABASE_BACKUP_COMPRESSION)
//			{
//				ZipOutputStream zipOutputStream;
//				(zipOutputStream = new ZipOutputStream((OutputStream) object)).setMethod(8);
//				zipOutputStream.setLevel(9);
//				zipOutputStream.setComment("L2jSunrise Schema Backup Utility\r\n\r\nBackup date: " + (new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss:SSS z")).format(date));
//				zipOutputStream.putNextEntry(new ZipEntry(BackupManagerConfigs.DATABASE_BACKUP_DATABASE_NAME + ".sql"));
//				object = zipOutputStream;
//			}
			byte[] arrayOfByte = new byte[4096];
			int i;
			int j;
			for (i = 0; (j = inputStream.read(arrayOfByte)) != -1; i += j)
				((FileOutputStream) object).write(arrayOfByte, 0, j);
			inputStream.close();
			((BufferedReader) object).close();
			if (i == 0)
			{
				file.delete();
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String str;
				while ((str = bufferedReader.readLine()) != null)
					a.info(DatabaseBackupManager.class.getSimpleName() + ": " + str);
				bufferedReader.close();
			}
			else
			{
				a.info(DatabaseBackupManager.class.getSimpleName() + ": DB `" + "l2trinityBackup" + "` backed up in " + ((System.currentTimeMillis() - date.getTime()) / 1000L) + " s.");
			}
			process.waitFor();
			return;
		}
		catch (Exception exception)
		{
			a.warning(DatabaseBackupManager.class.getSimpleName() + ": Could not make backup: " + exception.getMessage());
			return;
		}
	}
}
