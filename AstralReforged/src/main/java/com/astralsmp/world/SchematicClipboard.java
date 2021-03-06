package com.astralsmp.world;

import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.jetbrains.annotations.NotNull;

import java.io.*;


/**
 * Schematic clip board handling is done here!
 *
 * A bit of information can be found in the following links:
 *
 * * Reading a schematic file:
 * @See https://bukkit.org/threads/how-the-heck-do-i-read-a-schematic-file.45065/
 *
 * * Reading a schematic file:
 * @See https://www.spigotmc.org/threads/solved-how-to-read-schematics.63401/
 *
 * * Schematic file format:
 * @See https://minecraft.gamepedia.com/Schematic_file_format
 *
 * * Chunk format:
 * @See https://minecraft.gamepedia.com/Chunk_format
 *
 * * * Indexing a block from the array:
 * @See https://minecraft.gamepedia.com/Schematic_file_format#NBT_Structure
 *
 *
 *
 */
public class SchematicClipboard {

    private static final int PASTE_LIMIT = 16777216; //Limit for NMS schematic handling.
    private static final byte COMPOUND_ID = 10; //Type ID of the NBTTagCompound class found in source code.

    //Coordinates in schematics range from (0,0,0) -> (Width - 1, Height - 1, Length - 1).
    private int blockCount;
    private short width; //Size along the X axis.
    private short height; //Size along the Y axis.
    private short length; //Size along the Z axis.
    private byte[] blockIds; //Array of blocks ids in the schematic. 8 bits per block sorted as so y -> z -> x
    private byte[] blockData; //Array of the block data in the schematic. Only lower 4 bits used in each byte.
    private NBTTagList entities; //List of entities in the schematic.
    private NBTTagList tileEntities; //List of tile entities in the schematic.

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public short getWidth() {
        return width;
    }

    public void setWidth(short width) {
        this.width = width;
    }

    public short getHeight() {
        return height;
    }

    public void setHeight(short height) {
        this.height = height;
    }

    public short getLength() {
        return length;
    }

    public void setLength(short length) {
        this.length = length;
    }

    public byte[] getBlockIds() {
        return blockIds;
    }

    public void setBlockIds(byte[] blockIds) {
        this.blockIds = blockIds;
    }

    public byte[] getBlockData() {
        return blockData;
    }

    public void setBlockData(byte[] blockData) {
        this.blockData = blockData;
    }

    public NBTTagList getEntities() {
        return entities;
    }

    public void setEntities(NBTTagList entities) {
        this.entities = entities;
    }

    public NBTTagList getTileEntities() {
        return tileEntities;
    }

    public void setTileEntities(NBTTagList tileEntities) {
        this.tileEntities = tileEntities;
    }

    public SchematicClipboard(File file) throws IOException {
        loadSchematic(file);
    }

    /**
     * Used to make a fresh copy of another clipboard.
     *
     * @param original the original clipboard.
     */
    public SchematicClipboard(SchematicClipboard original) {
        setTileEntities(original.getTileEntities());
        setWidth(original.getWidth());
        setLength(original.getLength());
        setHeight(original.getHeight());
        setEntities(original.getEntities());
        setBlockIds(original.getBlockIds());
        setBlockData(original.getBlockData());
        setBlockCount(getBlockData().length);
    }

    /**
     * loads a schematic to the clipboard.
     *
     * @param file the file being loaded.
     * @throws IOException when an improper file is loaded.
     */
    public void loadSchematic(File file) throws IOException {
        final InputStream stream = openInputStream(file);
        final NBTTagCompound tagCompound = readSchematic(stream);
        setFields(tagCompound);
        stream.close();
    }

    /**
     * Sets global fields that are contained in the clipboard.
     *
     * @param tagCompound the NBTTagCompound object to retrieve data from.
     */
    private void setFields(final @NotNull NBTTagCompound tagCompound) {
        setWidth(tagCompound.getShort("Width"));
        setHeight(tagCompound.getShort("Height"));
        setLength(tagCompound.getShort("Length"));
        setBlockIds(tagCompound.getByteArray("Blocks"));
        setBlockData(tagCompound.getByteArray("Data"));
        setEntities(tagCompound.getList("Entities", COMPOUND_ID));
        setTileEntities(tagCompound.getList("TileEntities", COMPOUND_ID));
    }

    /**
     * Obfuscated methods isolated for readability. Simply uses NMS to
     * read data from a schematic file and returns the compound. NMS uses
     * a buffered stream so it is very efficient.
     *
     * @param inputStream the input stream of the schematic file.
     * @return the NamedBinaryTagCompound (NBTTagCompound) of the stream.
     * @throws IOException when pigs fly.
     */
    private NBTTagCompound readSchematic(@NotNull final InputStream inputStream) throws IOException {
        return NBTCompressedStreamTools.a(inputStream);
    }

    /**
     * Opens an input stream to a file for modification.
     *
     * @param file the target file.
     * @return an input stream for the file.
     */
    private @NotNull InputStream openInputStream(@NotNull final File file) throws FileNotFoundException {
        return new FileInputStream(file);
    }

    /**
     * Returns a shallow clone of the clipboard.
     * The shallow clone is a new SchematicClipboard
     * object. Fast, but it sacrifices deep copying.
     * Essentially, a new instance of the class is made
     * and the fields from the old one are copied to the
     * new one.
     *
     * @return a shallow clone of the clipboard.
     */
    public SchematicClipboard shallowClone() {
        return new SchematicClipboard(this);
    }

}