package de.julianweinelt.databench.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.databench.DataBench;
import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.ui.BenchUI;
import de.julianweinelt.databench.ui.editor.EditorTab;
import de.julianweinelt.databench.ui.editor.IEditorTab;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@SuppressWarnings("SpellCheckingInspection")
public class FileManager {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final File tempFolder = new File("tmp");

    public static FileManager instance() {
        return DataBench.getInstance().getFileManager();
    }

    public void save(List<IEditorTab> tabs, Project project) {
        File projectFolder = new File(tempFolder, project.getUuid().toString());
        if (projectFolder.mkdirs()) log.debug("Project folder created");
        File[] files1 = projectFolder.listFiles();
        if (files1 == null) return;
        Arrays.stream(files1).toList().forEach(file -> {
            if (file.delete()) log.debug("File deleted: {}", file.getAbsolutePath());
        });
        List<ProjectFile> files = new ArrayList<>();

        for (IEditorTab tab : tabs) {
            if (tab instanceof EditorTab e) {
                if (e.getSaveFile() != null) {
                    if (e.isFileSaved()) {
                        files.add(new ProjectFile(e.getSaveFile(), FileState.SAVED));
                    } else {
                        createTempFile(projectFolder, files, e);
                    }
                } else {
                    createTempFile(projectFolder, files, e);
                }
            }
        }

        try (FileWriter w = new FileWriter(new File(projectFolder, ".projroot"))) {
            w.write(GSON.toJson(files));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void createTempFile(File projectFolder, List<ProjectFile> files, EditorTab e) {
        UUID id = UUID.randomUUID();
        File file = new File(projectFolder, id + ".tmp");
        try (FileWriter w = new FileWriter(file)) {
            w.write(e.getEditorContent());
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        files.add(new ProjectFile(id, file, FileState.NON_SAVED));
    }

    public List<EditorTab> getProjectData(Project project, BenchUI ui) {
        List<EditorTab> tabs = new ArrayList<>();
        File projectFolder = new File(tempFolder, project.getUuid().toString());
        File[] files = projectFolder.listFiles();
        if (files == null) return new ArrayList<>();
        File rootFile = new File(projectFolder, ".projroot");
        List<ProjectFile> projectFiles = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(rootFile))) {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) sb.append(line).append("\n");

            Type type = new TypeToken<List<ProjectFile>>(){}.getType();
            projectFiles = GSON.fromJson(sb.toString(), type);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        for (ProjectFile f : projectFiles) {
            try (BufferedReader br = new BufferedReader(new FileReader(f.filePath))) {
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) sb.append(line);

                if (f.getState().equals(FileState.SAVED))
                    tabs.add(new EditorTab(null, sb.toString(), ui, new File(f.filePath)));
                else tabs.add(new EditorTab(sb.toString(), ui));
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        return tabs;
    }

    @Getter
    public static class ProjectFile {
        private final UUID uniqueId;
        private transient final File file;
        private final String filePath;
        private final FileState state;

        public ProjectFile(UUID uniqueId, File file, FileState state) {
            this.uniqueId = uniqueId;
            this.file = file;
            filePath = file.getPath();
            this.state = state;
        }

        public ProjectFile(File file, FileState state) {
            this.uniqueId = UUID.randomUUID();
            this.file = file;
            filePath = file.getPath();
            this.state = state;
        }
    }
    public enum FileState {
        SAVED,
        NON_SAVED
    }
}
