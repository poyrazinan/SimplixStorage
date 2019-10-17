package de.leonhard.storage.internal.base;

import de.leonhard.storage.internal.base.exceptions.InvalidSettingException;
import de.leonhard.storage.internal.enums.FileType;
import de.leonhard.storage.internal.enums.ReloadSettings;
import de.leonhard.storage.internal.utils.FileUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"UnusedReturnValue", "unused"})
@Getter
public abstract class FlatFile implements StorageBase, Comparable<FlatFile> {
    @Setter
    protected String pathPrefix;
    @Setter
    protected ReloadSettings reloadSettings = ReloadSettings.INTELLIGENT;
    protected FileData fileData = new FileData();
    protected File file;
    protected FileType fileType;
    private long lastModified;


    /**
     * Reread the content of our flat file
     */
    protected abstract void update();

    /**
     * Creates an empty .yml or .json file.
     *
     * @param file the file to be created.
     * @return true if file was created.
     */
    protected final synchronized boolean create(final File file) {
        this.fileType = FileTypeUtils.getFileType(file);
        this.file = file;
        return generateFile(file);
    }

    private synchronized boolean generateFile(File file) {
        if (file.exists()) {
            lastModified = System.currentTimeMillis();
            return false;
        } else {
            FileUtils.getAndMake(file);
            lastModified = System.currentTimeMillis();
            return true;
        }
    }

    public void reload() {
        try {
            if (shouldReload()) {
                update();
            }
        } catch (InvalidSettingException e) {
            e.printStackTrace();
        }
    }

    public void reload(boolean force) {
        try {
            if (shouldReload() || force) {
                update();
            }
        } catch (InvalidSettingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasKey(final String key) {
        String tempKey = (pathPrefix == null) ? key : pathPrefix + "." + key;
        reload();
        return fileData.containsKey(tempKey);
    }


    @Override
    public Set<String> singleLayerKeySet() {
        reload();
        return fileData.singleLayerKeySet();
    }

    @Override
    public Set<String> singleLayerKeySet(final String key) {
        reload();
        return fileData.singleLayerKeySet(key);
    }

    @Override
    public Set<String> keySet() {
        reload();
        return fileData.keySet();
    }

    @Override
    public Set<String> keySet(final String key) {
        reload();
        return fileData.keySet(key);
    }

    protected boolean shouldReload() throws InvalidSettingException {
        if (reloadSettings.equals(ReloadSettings.AUTOMATICALLY)) {
            return true;
        } else if (reloadSettings.equals(ReloadSettings.INTELLIGENT)) {
            return hasChanged();
        } else if (reloadSettings.equals(ReloadSettings.MANUALLY)) {
            return false;
        } else {
            throw new InvalidSettingException("No proper ReloadSetting");
        }
    }

    public final boolean hasChanged() {
        return FileUtils.hasChanged(file, lastModified);
    }

    public final String getName() {
        return this.file.getName();
    }

    public final String getFilePath() {
        return file.getAbsolutePath();
    }

    public void replace(final CharSequence target, final CharSequence replacement) throws IOException {
        final List<String> lines = Files.readAllLines(file.toPath());
        final List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(line.replace(target, replacement));
        }
        Files.write(file.toPath(), result);
    }

    protected final FlatFile getFlatFileInstance() {
        return this;
    }

    @Override
    public synchronized int compareTo(final FlatFile flatFile) {
        return this.file.compareTo(flatFile.file);
    }

    @Override
    public synchronized int hashCode() {
        return this.file.hashCode();
    }

    @Override
    public synchronized String toString() {
        return this.file.getAbsolutePath();
    }

    @Override
    public synchronized boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        } else {
            FlatFile flatFile = (FlatFile) obj;
            return this.pathPrefix.equals(flatFile.pathPrefix)
                    && reloadSettings.equals(flatFile.reloadSettings)
                    && this.fileData.equals(flatFile.fileData)
                    && this.file.equals(flatFile.file)
                    && fileType.equals(flatFile.fileType)
                    && this.lastModified == flatFile.lastModified;
        }
    }
}