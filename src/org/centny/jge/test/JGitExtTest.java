package org.centny.jge.test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.centny.jge.JGitExt;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class JGitExtTest {

	@Test
	public void testClone() throws Exception {
		FileUtils.deleteDirectory(new File("./Test/Tmp/jgd"));
		FileUtils.deleteDirectory(new File("./Test/Tmp/jgd2"));
		// FileUtils.moveDirectoryToDirectory(src, destDir, createDestDir)
		Git git;
		String fpath;
		Iterable<RevCommit> logs;
		fpath = "file://" + new File("./").getAbsolutePath()
				+ "/Test/Data/jgd.git";
		git = JGitExt.clone(new File("./Test/Tmp/jgd"), fpath, "master");
		logs = git.log().all().call();
		for (RevCommit rc : logs) {
			System.out.println(rc.getFullMessage());
		}
		fpath = "file://" + new File("./").getAbsolutePath()
				+ "/Test/Data/jgd2.git";
		git = JGitExt.clone(new File("./Test/Tmp/jgd2"), fpath, "master");
		logs = git.log().all().call();
		for (RevCommit rc : logs) {
			System.out.println(rc.getFullMessage());
		}

	}

	@Test
	public void testAddRemote() throws Exception {
		Git git = Git.open(new File("./Test/Tmp/jgd2"));
		JGitExt.addRemote(git.getRepository(), "am", "../jgd");
	}

	@Test
	public void testMerge() throws Exception {
		Git git = Git.open(new File("./Test/Tmp/jgd2"));
		MergeResult mres = JGitExt.mergeRemote(git, "am");
		// System.out.println(mres.getConflicts());
		Map<String, int[][]> cfs = JGitExt.loadConflict(git.getRepository());
		if (cfs != null) {
			for (String key : cfs.keySet()) {
				int[][] c = cfs.get(key);
				System.out.println("Conflicts in file " + key);
				for (int i = 0; i < c.length; ++i) {
					System.out.println("  Conflict #" + i);
					for (int j = 0; j < (c[i].length) - 1; ++j) {
						if (c[i][j] >= 0)
							System.out.println("    Chunk for "
									+ mres.getMergedCommits()[j]
									+ " starts on line #" + c[i][j]);
					}
				}
			}
		}
	}

	@Test
	public void testDiff() throws Exception {
		Git git = Git.open(new File("./Test/Tmp/jgd2"));
		List<DiffEntry> des = git.diff().call();
		List<DiffEntry> cdes = JGitExt.filter(des, ChangeType.MODIFY);
		for (DiffEntry de : cdes) {
			System.out.println(de.getNewPath());
			// DirCache dc = git.add().addFilepattern(de.getNewPath()).call();
			// System.out.println(dc.toString());
		}
		//
	}

	@Test
	public void testCommit() throws Exception {
		Git git = Git.open(new File("./Test/Tmp/jgd2"));
		RevCommit rc = git.commit().setMessage("test commit").call();
		System.out.println(rc.toString());
	}

	// @Test
	// public void testRebase() throws Exception {
	// Git git = Git.open(new File("./Test/Tmp/jgd2"));
	// git.rebase().runInteractively(handler)
	// }

	@Test
	public void testPull() throws Exception {
		Git git = Git.open(new File("./Test/Tmp/jgd"));
		PullResult pres = git.pull().call();
		System.out.println(pres.getFetchResult().getAdvertisedRefs());
		System.out.println(pres.getMergeResult().getMergeStatus());

	}

	@Test
	public void testCreate() {
		try {
			FileRepository frepo = new FileRepository("Test/Data/jgd3.git");
			frepo.create();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
