import com.formdev.flatlaf.FlatDarculaLaf
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.w3c.dom.Element
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.text.Document
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.exitProcess


class MainFrame : JFrame() {
    private lateinit var installClientButton: JButton
    private lateinit var installServerButton: JButton
    private lateinit var exitButton: JButton
    private lateinit var progressBar: JProgressBar
    private lateinit var progressLabel: JLabel
    private lateinit var loaderVersionBox: JTextField
    private lateinit var minecraftVersionBox: JTextField
    init {
        setTitle("Vulpes Mod Loader Installer")
        setSize(500, 300)
        isResizable = false
        setLocationRelativeTo(null)

        initComponent()
        initEvent()
    }

    private fun initComponent() {
        val banner = JLabel(ImageIcon(ImageIO.read(javaClass.classLoader.getResourceAsStream("vulpes-banner.png"))))
        banner.setBounds(0,0,300,120)
        val contentPane: Container = contentPane
        contentPane.setLayout(BorderLayout())
        contentPane.add(banner,BorderLayout.NORTH)
        val progressPane = JPanel(GridLayout(3,1))
        progressLabel = JLabel("To start, select a Vulpes version and a Minecraft version, then press install")
        progressBar = JProgressBar(0,100)
        progressPane.add(progressLabel)
        progressPane.add(progressBar)
        val installPane = JPanel(GridLayout(1,3))
        installClientButton = JButton("Install Client")
        installServerButton = JButton("Install Server (WIP)")
        exitButton = JButton("Exit")
        installPane.add(installClientButton)
        installPane.add(installServerButton)
        installPane.add(exitButton)
        progressPane.add(installPane)
        contentPane.add(progressPane,BorderLayout.SOUTH)
        val selectionPane = JPanel()
        selectionPane.layout = BoxLayout(selectionPane, BoxLayout.Y_AXIS)
        selectionPane.alignmentX = CENTER_ALIGNMENT
        selectionPane.alignmentY = CENTER_ALIGNMENT
        val label1 = JLabel("Vulpes Loader Version")
        loaderVersionBox = JTextField("main-SNAPSHOT")
        val grid1 = JPanel(GridBagLayout())
        loaderVersionBox.isEditable = true
        label1.alignmentX = Component.CENTER_ALIGNMENT
        grid1.add(label1)
        grid1.add(loaderVersionBox)
        selectionPane.add(grid1)
        val label2 = JLabel("Minecraft Version")
        minecraftVersionBox = JTextField("")
        val grid2 = JPanel(GridBagLayout())
        loaderVersionBox.isEditable = true
        label2.alignmentX = Component.CENTER_ALIGNMENT
        grid2.add(label2)
        grid2.add(minecraftVersionBox)
        selectionPane.add(grid2)
        contentPane.add(selectionPane,BorderLayout.CENTER)
        pack()
    }

    private fun initEvent() {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                exitProcess(0)
            }
        })
        installClientButton.addActionListener { e -> installClient(e) }
        installServerButton.addActionListener { _ -> JOptionPane.showMessageDialog(this,"Installation on the Minecraft Server JAR is currently unimplemented","Notice",JOptionPane.ERROR_MESSAGE) }
        exitButton.addActionListener { _ -> exitProcess(0) }
    }

    private fun installClient(event: ActionEvent) {
        SwingUtilities.invokeLater {
            Thread {
                installClientButton.isEnabled = false
                installServerButton.isEnabled = false
                exitButton.isEnabled = false
                minecraftVersionBox.isEnabled = false
                loaderVersionBox.isEnabled = false
                progressBar.isIndeterminate = true
                progressLabel.text = "Searching for Minecraft Instance..."
                // Search for specified Minecraft Version
                val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
                var minecraftPath: String = if (os.contains("win")) {
                    System.getenv("APPDATA") + File.separator + ".minecraft"
                } else if (os.contains("mac")) {
                    System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft"
                } else {
                    System.getProperty("user.home") + File.separator + ".minecraft"
                }
                if (!File(minecraftPath).exists()) {
                    abortInstall("You don't appear to have the Minecraft Launcher installed (or you've never opened it) please open it and install the version you want to install Vulpes Loader to")
                    return@Thread
                }
                progressLabel.text = "Searching for Minecraft Version..."
                progressLabel.text = "Creating Temporary Download Directory..."
                val tempDir = File(System.getProperty("java.io.tmpdir") + File.separator + "VulpesInstaller")
                tempDir.deleteRecursively()
                tempDir.mkdir()
                progressLabel.text = "Downloading Deobfuscator..."
                //
                "buildscript {\nrepositories {\nmaven {\nurl \"https://repo.spongepowered.org/repository/maven-public/\"\n}\n}\ndependencies {\nclasspath \"org.spongepowered:vanillagradle:0.2.1-SNAPSHOT\"\n}\n}\nplugins {\nid 'java'\n}\napply plugin: \"org.spongepowered.gradle.vanilla\"\ngroup = 'com.example'\nversion = \"1.0.0\"\nrepositories {\nmavenCentral()\nmaven { url = \"https://repo.spongepowered.org/maven/\" }\nmaven { url = \"https://maven.minecraftforge.net/\" }\nmaven { url = \"https://maven.enaium.cn\" }\nmaven { url = 'https://jitpack.io' }\n}\ndependencies {\nimplementation 'com.github.VulpesMC:VulpesStandardLibrary:main-SNAPSHOT'\nimplementation 'com.github.VulpesMC:VulpesLoader:main-SNAPSHOT'\n}\nminecraft {\nversion(\""+minecraftVersionBox.text+"\")\n}"
                try {
                    BufferedInputStream(URL("https://github.com/Fox2Code/Repacker/releases/download/v1.4.0/Repacker-1.4.0.jar").openStream()).use { `in` ->
                        FileOutputStream(tempDir.absolutePath + File.separator + "Repacker-1.4.0.jar").use { fileOutputStream ->
                            val dataBuffer = ByteArray(1024)
                            var bytesRead: Int
                            while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                fileOutputStream.write(dataBuffer, 0, bytesRead)
                            }
                        }
                    }
                } catch (e: IOException) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                progressLabel.text = "Deobfuscating Minecraft..."
                if(JOptionPane.showConfirmDialog(this,"Please accept the following copyright notice to continue:\n\n(c) 2020 Microsoft Corporation. These mappings are provided \"as-is\" and you bear the risk of using them. You may copy and use the mappings for development purposes, but you may not redistribute the mappings complete and unmodified. Microsoft makes no warranties, express or implied, with respect to the mappings provided here.  Use and modification of this document or the source code (in any form) of Minecraft: Java Edition is governed by the Minecraft End User License Agreement available at https://account.mojang.com/documents/minecraft_eula.","Deobfuscation Notice",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
                    abortInstall("User denied the copyright notice for the Mojang Mappings\nCannot continue");
                    return@Thread
                }
                try {
                    val javaHome = System.getProperty("java.home")
                    val javaBin = javaHome +
                            File.separator + "bin" +
                            File.separator + "java"
                    val pb = ProcessBuilder(javaBin, "-jar", tempDir.absolutePath + File.separator + "Repacker-1.4.0.jar", tempDir.absolutePath, minecraftVersionBox.text)
                    if(pb.start().waitFor() != 0) {
                        tempDir.deleteRecursively()
                        abortInstall("Repacker exited with non-zero value!")
                        return@Thread
                    }
                } catch(e: Exception) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                progressLabel.text = "Downloading VulpesLoader..."
                try {
                    BufferedInputStream(URL("https://jitpack.io/com/github/VulpesMC/VulpesLoader/"+loaderVersionBox.text+"/VulpesLoader-"+loaderVersionBox.text+"-all.jar").openStream()).use { `in` ->
                        FileOutputStream(tempDir.absolutePath + File.separator + "VulpesLoader.jar").use { fileOutputStream ->
                            val dataBuffer = ByteArray(1024)
                            var bytesRead: Int
                            while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                fileOutputStream.write(dataBuffer, 0, bytesRead)
                            }
                        }
                    }
                } catch (e: IOException) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                progressLabel.text = "Extracting VulpesLoader Files..."
                try {
                    val dir = File(tempDir.absolutePath + File.separator + "VulpesLoader")
                    dir.deleteRecursively()
                    dir.mkdir()
                    val pb = ProcessBuilder("jar", "xf", ".."+File.separator+"VulpesLoader.jar")
                    pb.directory(dir)
                    if(pb.start().waitFor() != 0) {
                        tempDir.deleteRecursively()
                        abortInstall("Java SDK's \"jar\" exited with non-zero value!")
                        return@Thread
                    }
                } catch(e: Exception) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                progressLabel.text = "Adding VulpesLoader Classes to Deobfuscated Minecraft JAR..."
                try {
                    val dir = File(tempDir.absolutePath + File.separator + "VulpesLoader")
                    val pb = ProcessBuilder("jar", "uf", ".."+File.separator+"minecraft-"+minecraftVersionBox.text+"-remapped.jar", ".")
                    pb.directory(dir)
                    if(pb.start().waitFor() != 0) {
                        tempDir.deleteRecursively()
                        abortInstall("Java SDK's \"jar\" exited with non-zero value!")
                        return@Thread
                    }
                } catch(e: Exception) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                progressLabel.text = "Creating Minecraft Version Directory..."
                var ver = File(minecraftPath+File.separator+"versions"+File.separator+minecraftVersionBox.text+"-vulpes")
                ver.deleteRecursively()
                ver.mkdir()
                progressLabel.text = "Creating Manifest JSON..."
                var jsonObj = Gson().fromJson(tempDir.resolve(minecraftVersionBox.text+".json").readText(), JsonObject::class.java)
                jsonObj.remove("downloads")
                jsonObj.add("mainClass",JsonPrimitive("net.minecraft.launchwrapper.Launch"))
                jsonObj.add("id",JsonPrimitive(jsonObj.get("id").asString!!+"-vulpes"))
                ver.resolve(minecraftVersionBox.text+"-vulpes.json").writeText(Gson().toJson(jsonObj))
                progressLabel.text = "Moving Deobfuscated Minecraft JAR to Version Directory..."
                tempDir.resolve("minecraft-"+minecraftVersionBox.text+"-remapped.jar").renameTo(ver.resolve(minecraftVersionBox.text+"-vulpes.jar"))
                progressLabel.text = "Cleaning up..."
                tempDir.deleteRecursively()
                progressLabel.text = "Installation Successful!"
                if (JOptionPane.showConfirmDialog(
                        this,
                        "Vulpes Loader has been successfully installed!\nWould you like to add a new profile for Vulpes?",
                        "Installation Successful!",
                        JOptionPane.YES_NO_OPTION
                    ) == JOptionPane.YES_OPTION
                ) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Profile was successfully created\nIf the Minecraft Launcher is currently running then you'll need to restart it to see the new profile",
                        "Successfully Added Profile!",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
                progressBar.isIndeterminate = false
                installClientButton.isEnabled = true
                installServerButton.isEnabled = true
                exitButton.isEnabled = true
                minecraftVersionBox.isEnabled = true
                loaderVersionBox.isEnabled = true
                progressLabel.text = "To start, select a Vulpes version and a Minecraft version, then press install"
            }.start()
        }
    }

    private fun abortInstall(reason: String) {
        JOptionPane.showMessageDialog(this,reason,"Installation Failed!",JOptionPane.ERROR_MESSAGE)
        installClientButton.isEnabled = true
        installServerButton.isEnabled = true
        exitButton.isEnabled = true
        minecraftVersionBox.isEnabled = true
        loaderVersionBox.isEnabled = true
        progressBar.isIndeterminate = false
        progressLabel.text = "To start, select a Vulpes version and a Minecraft version, then press install"
    }
}

fun main(args: Array<String>) {
    JFrame.setDefaultLookAndFeelDecorated( true );
    JDialog.setDefaultLookAndFeelDecorated( true );
    System.setProperty("flatlaf.useWindowDecorations","true")
    System.setProperty("flatlaf.menuBarEmbedded","true")
    FlatDarculaLaf.setup()
    var f = MainFrame()
    f.isVisible = true
}