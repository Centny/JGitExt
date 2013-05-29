package org.centny.jge.test;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.centny.jge.JGitExt;
import org.centny.jge.amerge.AutoMerge;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.Test;

public class AutoMergeTest {

	@Test
	public void testPullL2R() throws Exception {
		File wsdir = new File("./Test/Tmp/amjgd");
		FileUtils.deleteDirectory(wsdir);
		AutoMerge am = new AutoMerge(wsdir);
		String fpath;
		fpath = "file://" + new File("./").getAbsolutePath()
				+ "/Test/Data/jgd.git";
		am.cloneLocal(fpath, "master");
		fpath = "file://" + new File("./").getAbsolutePath()
				+ "/Test/Data/jgd2.git";
		am.cloneRemote(fpath, "master");
		am.initAMerge();
		Assert.assertFalse(am.pullL2R().isEmpty());
	}

	@Test
	public void testPullR2L() throws Exception {
		File wsdir = new File("./Test/Tmp/amjgd");
		FileUtils.deleteDirectory(wsdir);
		AutoMerge am = new AutoMerge(wsdir);
		String fpath;
		fpath = "file://" + new File("./").getAbsolutePath()
				+ "/Test/Data/jgd2.git";
		am.cloneLocal(fpath, "master");
		fpath = "file://" + new File("./").getAbsolutePath()
				+ "/Test/Data/jgd4.git";
		am.cloneRemote(fpath, "master");
		am.initAMerge();
		 Assert.assertFalse(am.pullR2L().isEmpty());
	}

	@Test
	public void testConflict() throws IOException {
		File wsdir = new File("./Test/Tmp/amjgd");
		AutoMerge am = new AutoMerge(wsdir);
		Assert.assertTrue(am.isConflict());
		// System.out.println(am.isConflict());
	}

	@Test
	public void testCone() {
		String fpath = "file://" + new File("./").getAbsolutePath()
				+ "/Test/Data/jgd3.git";
		try {
			JGitExt.clone(new File("/tmp/jgd3"), fpath, "master");
		} catch (InvalidRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
