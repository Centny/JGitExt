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
			if (!line.trim().matches("\\[\\b*remote\\b*.*\\]")) {
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

	public static void assertTrue(boolean bool) {
		if (!bool) {
			throw new RuntimeException("Assertion faild");
		}
	}
}
