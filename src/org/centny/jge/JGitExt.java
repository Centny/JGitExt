package org.centny.jge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;

public class JGitExt {
	public static Git clone(File dir, String uri)
			throws InvalidRemoteException, TransportException, GitAPIException {
		Git re = null;
		CloneCommand command = Git.cloneRepository();
		command.setDirectory(dir);
		command.setURI(uri);
		re = command.call();
		return re;
	}

	public static Git clone(File dir, String uri, String branch)
			throws InvalidRemoteException, TransportException, GitAPIException {
		Git re = null;
		CloneCommand command = Git.cloneRepository();
		command.setBranch("refs/heads/" + branch);
		command.setDirectory(dir);
		command.setURI(uri);
		re = command.call();
		return re;
	}

	public static void addRemote(Repository repo, String name, String remote)
			throws InvalidParameterException, IOException {
		File dir = repo.getDirectory();
		File cfg = new File(dir, "config");
		InputStreamReader is = new InputStreamReader(new FileInputStream(cfg),
				"UTF-8");
		BufferedReader reader = new BufferedReader(is);
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.trim().matches("\\[\\s*remote\\s*.*\\]")) {
				continue;
			}
			if (line.indexOf(name) > -1) {
				reader.close();
				throw new InvalidParameterException("the remote name " + name
						+ " is aleady added");
			}
		}
		reader.close();
		OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(
				cfg, true));
		BufferedWriter writer = new BufferedWriter(os);
		writer.append("[remote \"" + name + "\"]\n");
		writer.append("\turl = " + remote + "\n");
		writer.append("\tfetch = +refs/heads/*:refs/remotes/" + name + "/*\n");
		writer.close();
	}

	public static List<DiffEntry> filter(List<DiffEntry> src, ChangeType ctype) {
		List<DiffEntry> dst = new ArrayList<DiffEntry>();
		for (DiffEntry de : src) {
			if (ctype.equals(de.getChangeType())) {
				dst.add(de);
			}
		}
		return dst;
	}

	public static MergeResult mergeRemote(Git git, String name)
			throws InvalidRemoteException, TransportException, GitAPIException,
			IOException {
		FetchResult fres = git.fetch().setRemote(name).call();
		System.out.println(fres.getMessages());
		MergeResult mres;
		Ref rref = git.getRepository().getRef(
				"refs/remotes/" + name + "/master");
		mres = git.merge().include(rref).call();
		if (mres.getMergeStatus().equals(MergeStatus.CONFLICTING)) {
			storeConflict(git.getRepository(), mres.getConflicts());
		}
		return mres;
	}

	public static void storeConflict(Repository repo,
			Map<String, int[][]> conflict) throws FileNotFoundException,
			IOException {
		File cfile = new File(repo.getDirectory(), "conflict");
		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(
				cfile));
		os.writeObject(conflict);
		os.close();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, int[][]> loadConflict(Repository repo) {
		ObjectInputStream is = null;
		try {
			File cfile = new File(repo.getDirectory(), "conflict");
			is = new ObjectInputStream(new FileInputStream(cfile));
			Object obj = is.readObject();
			if (obj instanceof Map<?, ?>) {
				return (Map<String, int[][]>) obj;
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
			}
		}
	}

	public static Boolean isConflicted(Repository db) {
		Map<String, int[][]> c = loadConflict(db);
		if (c == null || c.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	public static Boolean isExported(Repository db) {
		return new File(db.getDirectory(), "git-daemon-export-ok").exists();
	}

	public static Boolean markExport(Repository db) throws IOException {
		if (isExported(db)) {
			return true;
		}
		return new File(db.getDirectory(), "git-daemon-export-ok")
				.createNewFile();
	}

	public static Boolean markUnexport(Repository db) {
		return new File(db.getDirectory(), "git-daemon-export-ok").delete();
	}

	public static boolean authRepository(Repository db, String tname,
			boolean t4role, boolean isAdd) {
		if (isAdd && isAuthorizedOk(db, tname, t4role)) {
			return true;
		}
		File auth = new File(db.getDirectory(), ".authorized");
		if (!auth.exists() && !isAdd) {
			return false;
		}
		FileInputStream is = null;
		BufferedReader reader = null;
		StringBuffer sbuf = new StringBuffer();
		boolean isApped = false;
		try {
			is = new FileInputStream(auth);
			reader = new BufferedReader(new InputStreamReader(is));
			String line = null;
			boolean isRole = false;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				if (line.equals("[role]")) {
					isRole = true;
					sbuf.append(line + "\n");
					continue;
				} else if (line.equals("[user]")) {
					isRole = false;
					sbuf.append(line + "\n");
					continue;
				}
				if (isAdd) {
					sbuf.append(line);
					if (isApped) {
						continue;
					}
					if ((isRole && t4role) || (!isRole && !t4role)) {
						sbuf.append(tname + "\n");
						isApped = true;
					}
				} else {
					if ((isRole && t4role) || (!isRole && !t4role)) {
						if (line.equals(tname)) {
							continue;
						} else {
							sbuf.append(line + "\n");
						}
					}
				}
			}
		} catch (Exception e) {
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (Exception e) {
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e) {
			}
		}
		if (!isApped && isAdd) {
			if (t4role) {
				sbuf.append("[role]\n");
			} else {
				sbuf.append("[user]\n");
			}
			sbuf.append(tname + "\n");
		}
		FileOutputStream os = null;
		BufferedWriter writer = null;
		try {
			os = new FileOutputStream(auth);
			writer = new BufferedWriter(new OutputStreamWriter(os));
			writer.write(sbuf.toString());
			writer.flush();
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (Exception e) {
			}
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (Exception e) {
			}
		}
	}

	public static boolean isAuthorizedOk(Repository db, String tname,
			boolean c4role) {
		File auth = new File(db.getDirectory(), ".authorized");
		if (!auth.exists()) {
			return false;
		}
		FileInputStream is = null;
		BufferedReader reader = null;
		try {
			is = new FileInputStream(auth);
			reader = new BufferedReader(new InputStreamReader(is));
			String line = null;
			boolean isRole = false;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				if (line.equals("[role]")) {
					isRole = true;
					continue;
				} else if (line.equals("[user]")) {
					isRole = false;
					continue;
				}
				if (isRole && c4role) {
					if (line.equals(tname)) {
						return true;
					} else {
						continue;
					}
				} else {
					if (line.equals(tname)) {
						return true;
					} else {
						continue;
					}
				}
			}
		} catch (Exception e) {
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (Exception e) {
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e) {
			}
		}
		return false;
	}

	public static void assertTrue(boolean bool) {
		if (!bool) {
			throw new RuntimeException("Assertion faild");
		}
	}

	public static String createRepository(File dir, String uri, String branch)
			throws Exception {
		File tmp = new File(dir, "tmp");
		tmp.mkdirs();
		try {
			JGitExt.clone(tmp, uri, branch);
			File git = new File(tmp, ".git");
			FileUtils.copyDirectory(git, dir);
			return "Ok";
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				FileUtils.deleteDirectory(tmp);
			} catch (IOException e) {
			}
		}

	}
}
