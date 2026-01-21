package de.julianweinelt.databench.api;

import lombok.Getter;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class FileWatcher {
    private final int UPDATE_INTERVAL = 1;
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    private final File path;
    private final IFileWatcherListener listener;
    private FileTree tree;

    public FileWatcher(File path, IFileWatcherListener listener) {
        this.path = path;
        this.listener = listener;
        startScheduler();
    }

    private void startScheduler() {
        service.scheduleAtFixedRate(() -> {
            if (scanTree()) listener.fileTreeUpdate(tree);

        }, UPDATE_INTERVAL, UPDATE_INTERVAL, TimeUnit.SECONDS);
    }

    public boolean scanTree() {
        List<FileObject> objs = new ArrayList<>();
        File[] fi = path.listFiles();
        if (fi == null) return false;
        for (File f : fi) {
            if (f.isDirectory()) {
                FileObject o = getFolder(f);
                objs.add(o);
            } else {
                FileObject o = getFile(f);
                objs.add(o);
            }
        }
        FileTree temp = new FileTree(objs);
        if (temp.equals(tree)) {
            tree = new FileTree(objs);
            return false;
        } else {
            tree = new FileTree(objs);
            return true;
        }
    }

    private FileObject getFolder(File file) {
        File[] files = file.listFiles();
        FileObject o = new FileObject(file.getPath(), file.getName(), file.isDirectory(), new ArrayList<>(), FileType.FOLDER);
        if (files == null) return o;
        for (File f : files) {
            if (!f.isDirectory())
                o.children().add(getFile(f));
            else o.children().add(getFolder(f));
        }
        return o;
    }

    private FileObject getFile(File file) {
        return new FileObject(file.getPath(), file.getName(), file.isDirectory(), new ArrayList<>(), getFileType(file));
    }

    public void createTree(DefaultMutableTreeNode root) {
        for (FileObject o : tree.data()) {
            DefaultMutableTreeNode sub = new DefaultMutableTreeNode(o);
            if (o.directory()) addSubTree(sub, o);
            root.add(sub);
        }
    }
    private void addSubTree(DefaultMutableTreeNode node, FileObject obj) {
        for (FileObject o : obj.children()) {
            DefaultMutableTreeNode sub = new DefaultMutableTreeNode(o);
            if (o.directory()) {
                addSubTree(sub, o);
            }
            node.add(sub);
        }
    }

    private FileType getFileType(File f) {
        if (f.isDirectory()) return FileType.FOLDER;
        else {
            String n = f.getName().toLowerCase();
            if (n.endsWith("sql")) return FileType.SQL;
            if (n.endsWith("json")) return FileType.JSON;
            if (n.endsWith("txt")) return FileType.TXT;
            if (n.endsWith("csv")) return FileType.CSV;
            if (n.endsWith("xls")) return FileType.XLS;
            if (n.endsWith("xlsx")) return FileType.XLSX;
        }
        return FileType.UNKNOWN;
    }
}