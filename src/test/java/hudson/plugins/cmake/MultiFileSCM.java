package hudson.plugins.cmake;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.scm.NullSCM;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jvnet.hudson.test.SingleFileSCM;

/*
 *  Stolen from cppncss-plugin
 */

/**
 *
 * @author Pascal Martin
 *
 */
public class MultiFileSCM extends NullSCM {
    private List<SingleFileSCM> files = new ArrayList<SingleFileSCM>();

    public MultiFileSCM(List<SingleFileSCM> files) {
        this.files = files;
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher,
            FilePath workspace, BuildListener listener, File changeLogFile)
            throws IOException, InterruptedException {
        for (SingleFileSCM file : this.files) {
            file.checkout(build, launcher, workspace, listener, changeLogFile);
        }
        return true;
    }
}
