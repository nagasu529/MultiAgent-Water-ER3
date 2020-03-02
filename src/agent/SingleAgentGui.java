package agent;

import examples.content.mso.elements.Single;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class SingleAgentGui extends JFrame {
    //Farming agent class
    private SingleAgent myAgent;

    //GUI design preferences

    private JTextArea log;

    SingleAgentGui(SingleAgent a) {
        super(a.getLocalName());
        myAgent = a;

        //Combobox ET0 preference and action listener.

        String[] etListStrings = {"ET0-Spring", "ET0-Summer", "ET0-Autumn", "ET0-Winter"};

        JPanel controls = new JPanel();
        getContentPane().add(controls, BorderLayout.NORTH);

        //log area create
        log = new JTextArea(5, 20);
        log.setEditable(false);
        getContentPane().add(log, BorderLayout.CENTER);
        log.setMargin(new Insets(5, 5, 300, 500));
        JScrollPane logScrollPane = new JScrollPane(log);
        getContentPane().add(logScrollPane, BorderLayout.CENTER);

        // Make the agent terminate when the user closes
        // the GUI using the button on the upper right corner
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });
        setResizable(false);
    }

    public void show() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.show();
    }

    public void displayUI(String displayUI) {
        log.append(displayUI);
        log.setCaretPosition(log.getDocument().getLength());
    }
}