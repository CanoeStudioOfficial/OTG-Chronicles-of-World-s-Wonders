package com.otg.worldpacker;

import com.otg.worldodditieschronicles.Tags;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.InputStream;
import java.util.*;

import java.io.*;
import java.net.*;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class WorldPacker {

    public static Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @EventHandler
    public void load(FMLInitializationEvent event)
    {
        try
        {
            // Fetch the world files from this mod's own jar
            JarFile jarFile = new JarFile(FMLCommonHandler.instance().findContainerFor(this).getSource());
            Enumeration<JarEntry> entries = jarFile.entries();

            String worldName = null;
            ArrayList<JarEntry> srcWorldFilesInJar = new ArrayList<JarEntry>();

            while(entries.hasMoreElements())
            {
                JarEntry jarEntry = entries.nextElement();
                if(jarEntry.getName().startsWith("assets/worldpacker/") || jarEntry.getName().startsWith("assets\\worldpacker\\"))
                {
                    if(jarEntry.isDirectory() && !jarEntry.getName().equals("assets/worldpacker") && !jarEntry.getName().equals("assets\\worldpacker"))
                    {
                        File file = new File(jarEntry.getName());
                        File parentFile = file.getParentFile();
                        String parentFileName = parentFile.getAbsolutePath();
                        if((parentFileName.endsWith("assets\\worldpacker") || parentFileName.endsWith("assets/worldpacker")) && worldName == null) // TODO: Check for each world if it already exists, not only the first one.
                        {
                            worldName = jarEntry.getName().replace("assets/worldpacker/", "").replace("/", "").replace("assets\\worldpacker\\", "").replace("\\", "");
                        }
                    }
                    srcWorldFilesInJar.add(jarEntry);
                }
            }

            //Check the version within the jar file

            int[] arr = new int[] {0,0,0,0};

            InputStream iis = getClass().getClassLoader().getResourceAsStream("assets/worldpacker/" + worldName + "/version.txt");
            if (iis != null) {

                BufferedReader br = new BufferedReader(new InputStreamReader(iis));

                try {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                        line = br.readLine();

                        if ((line != null) && (line.contains("MajorVersion"))) {
                            arr[0] = Integer.parseInt(line.split(":")[1].trim());
                        }
                        if ((line != null) && (line.contains("MinorVersion"))) {
                            arr[1] = Integer.parseInt(line.split(":")[1].trim());
                        }
                    }
                    String everything = sb.toString();
                } finally {
                    br.close();
                }

            }

            //Check if preset is already installed and if so check the version.
            if(worldName != null && srcWorldFilesInJar.size() > 0) {


                File Version = new File(new File(".").getCanonicalPath() + File.separator + "mods" + File.separator + "OpenTerrainGenerator" + File.separator + "worlds" + File.separator + worldName + File.separator + "version.txt");

                if (Version.exists()) {

                    BufferedReader br = new BufferedReader(new FileReader(Version));
                    try {
                        StringBuilder sb = new StringBuilder();
                        String line = br.readLine();

                        String MajorV = "";
                        String MinorV = "";

                        while (line != null) {
                            sb.append(line);
                            sb.append(System.lineSeparator());
                            line = br.readLine();

                            if ((line != null) && (line.contains("MajorVersion"))) {
                                arr[2] = Integer.parseInt(line.split(":")[1].trim());
                            }
                            if ((line != null) && (line.contains("MinorVersion"))) {
                                arr[3] = Integer.parseInt(line.split(":")[1].trim());
                            }

                        }

                        String everything = sb.toString();
                    } finally {
                        br.close();
                    }
                }

                //Check if jar version is greater then installed version and if so update installed version.
                // arr[0] = Major Internal Version | arr[1] = Minor Internal Version
                // arr[2] = Major External Version | arr[3] = Minor External Version


                if(arr[0] > arr[2] || (arr[0] == arr[2] && arr[1] > arr[3]) || (!Version.exists())) {
                    if(!Version.exists()) {
                        LOGGER.info("Preset not detected, initiating new install, extracting world files for preset " + worldName + "...");
                    }
                    if(arr[0] > arr[2] || (arr[0] == arr[2] && arr[1] > arr[3])) {
                        LOGGER.info("More recent jar detected, will update preset " + worldName + " from version: " + arr[2] + "." + arr[3] +" to version: " + arr[0] + "." + arr[1]);
                    }

                    //Unpack preset to directories
                    for(JarEntry jarEntry : srcWorldFilesInJar)
                    {
                        File f = new File(new File(".").getCanonicalPath() + File.separator + "mods" + File.separator + "OpenTerrainGenerator" + File.separator + "worlds" + File.separator + jarEntry.getName().replace("assets/worldpacker", "").replace("assets\\worldpacker", ""));
                        if(jarEntry.isDirectory())

                        {
                            f.mkdirs();
                        } else {
                            if (!f.toString().contains(Version.toString())) {
                                f.createNewFile();
                            } else {
                                // Make sure version.txt is always unpacked first.
                                // This makes sure that the player does not end up with a corrupt installation on early exit whilst unpacking
                                Version.createNewFile();
                            }
                            FileOutputStream fos = new FileOutputStream(f);
                            byte[] byteArray = new byte[1024];
                            int i;
                            java.io.InputStream is = jarFile.getInputStream(jarEntry);
                            while ((i = is.read(byteArray)) > 0) {
                                fos.write(byteArray, 0, i);
                            }
                            is.close();
                            fos.close();
                        }
                    }
                    LOGGER.info("Completed Update Progress " + worldName + " is now version " + arr[0] + "." + arr[1]);
                } else {
                    LOGGER.info("Preset " + worldName + " version " + arr[0] + "." + arr[1] + " detected, nothing to do.");
                }
            }
            jarFile.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
