package org.centny.jge.test;

import java.io.File;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.centny.jge.amerge.AutoMerge;
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
				+ "/Test/Data/jgd.git";
		am.cloneLocal(fpath, "master");
		fpath = "file://" + new File("./").getAbsolutePath()
				+ "/Test/Data/jgd2.git";
		am.cloneRemote(fpath, "master");
		am.initAMerge();
		Assert.assertFalse(am.pullR2L().isEmpty());
	}

	@Test
	public void testConflict() {
		File wsdir = new File("./Test/Tmp/amjgd");
		AutoMerge am = new AutoMerge(wsdir);
		Assert.assertTrue(am.isConflict());
		// System.out.println(am.isConflict());
	}
}
