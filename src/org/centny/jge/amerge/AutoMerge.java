package org.centny.jge.amerge;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Properties;

import junit.framework.Assert;

import org.centny.jge.JGitExt;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;

public class AutoMerge {
	private File wsDir;
	private Properties cfg;
	private Git local, remote;

	public AutoMerge(File wsDir) {
		this.wsDir = wsDir;
		if (this.wsDir.exists()) {
			Assert.assertTrue(this.wsDir.isDirectory());
		} else {
			Assert.assertTrue(this.wsDir.mkdirs());
		}
		this.cfg = new Properties();
		try {
			this.cfg.load(new FileInputStream(new File(this.wsDir,
					"amerge.properties")));
		} catch (Exception e) {
		}
	}

	public File getWsDir() {
		return wsDir;
	}

	public Git getLocal() {
		try {
			if (this.local == null) {
				File ldir = new File(this.wsDir, "local");
				if (ldir.exists()) {
					this.local = Git.open(ldir);
				}
			}
		} catch (Exception e) {
		}
		return local;
	}

	public Git getRemote() {
		try {
			if (this.remote == null) {
				File rdir = new File(this.wsDir, "remote");
				if (rdir.exists()) {
					this.remote = Git.open(rdir);
				}
			}
		} catch (Exception e) {
		}
		return remote;
	}

	public Git cloneLocal(String uri, String branch)
			throws InvalidRemoteException, TransportException, GitAPIException {
		File ldir = new File(this.wsDir, "local");
		if (ldir.exists()) {
			Assert.assertTrue(ldir.delete());
		}
		this.local = JGitExt.clone(ldir, uri, branch);
		return this.local;
	}

	public Git cloneRemote(String uri, String branch)
			throws InvalidRemoteException, TransportException, GitAPIException {
		File rdir = new File(this.wsDir, "remote");
		if (rdir.exists()) {
			Assert.assertTrue(rdir.delete());
		}
		this.remote = JGitExt.clone(this.wsDir, uri, branch);
		return this.remote;
	}

	public void initAMerge() throws Exception {
		if (this.getLocal() == null || this.getRemote() == null) {
			throw new InvalidParameterException(
					"local or remote repository not inited");
		}
		JGitExt.addRemote(this.getLocal().getRepository(), "amerge",
				"../remote");
		JGitExt.addRemote(this.getRemote().getRepository(), "amerge",
				"../local");
	}

	public MergeResult mergeL2R() throws InvalidRemoteException,
			TransportException, GitAPIException, IOException {
		return JGitExt.mergeRemote(this.remote, "amerge");
	}

	public MergeResult mergeR2L() throws InvalidRemoteException,
			TransportException, GitAPIException, IOException {
		return JGitExt.mergeRemote(this.local, "amerge");
	}

	public void localCPush(String msg) throws NoHeadException,
			NoMessageException, UnmergedPathsException,
			ConcurrentRefUpdateException, WrongRepositoryStateException,
			GitAPIException {
		this.local.commit().setMessage(msg).call();
		this.local.push().setPushAll().call();
	}

	public void remoteCPush(String msg) throws NoHeadException,
			NoMessageException, UnmergedPathsException,
			ConcurrentRefUpdateException, WrongRepositoryStateException,
			GitAPIException {
		this.remote.commit().setMessage(msg).call();
		this.remote.push().setPushAll().call();
	}

	public Boolean isConflict() {
		if (this.local == null || this.remote == null) {
			return false;
		}
		return JGitExt.isConflicted(this.local.getRepository())
				|| JGitExt.isConflicted(this.remote.getRepository());
	}
}
