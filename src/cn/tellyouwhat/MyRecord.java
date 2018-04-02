package cn.tellyouwhat;

import com.baidu.speech.restapi.asrdemo.AsrMain;
import com.baidu.speech.restapi.common.DemoException;
import com.melloware.jintellitype.JIntellitype;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;

public class MyRecord extends JFrame implements ActionListener {

    //定义录音格式
    private AudioFormat audioFormat = null;
    //定义目标数据行,可以从中读取音频数据,该 TargetDataLine 接口提供从目标数据行的缓冲区读取所捕获数据的方法。
    private TargetDataLine targetDataLine = null;
    //定义源数据行,源数据行是可以写入数据的数据行。它充当其混频器的源。应用程序将音频字节写入源数据行，这样可处理字节缓冲并将它们传递给混频器。
    private SourceDataLine sourceDataLine = null;
    //定义字节数组输入输出流
    private ByteArrayInputStream inputStream = null;
    private ByteArrayOutputStream outputStream = null;
    //定义音频输入流
    private AudioInputStream audioInputStream = null;
    //定义停止录音的标志，来控制录音线程的运行
    private Boolean stopFlag = false;

    private JButton captureBtn, stopBtn, playBtn;

    private static final int BEGIN_TALK = 1;
    private static final int END_TALK = 2;
    private final JLabel jLabel;

    public static void main(String[] args) {

        //创造一个实例
        MyRecord myRecord = new MyRecord();
        myRecord.setAlwaysOnTop(true);
        myRecord.setResizable(false);
    }

    //构造函数
    public MyRecord() {
        //组件初始化
        //定义所需要的组件
        JPanel jp1 = new JPanel();
        JPanel jp2 = new JPanel();
        JPanel jp3 = new JPanel();

        jLabel = new JLabel("语音输入法");
        jLabel.setFont(new Font("微软雅黑", Font.BOLD, 40));
        jp1.add(jLabel);

        captureBtn = new JButton("开始讲话");
        //对开始录音按钮进行注册监听
        captureBtn.addActionListener(this);
        captureBtn.setActionCommand("captureBtn");
        //对停止录音进行注册监听
        stopBtn = new JButton("停止讲话");
        stopBtn.addActionListener(this);
        stopBtn.setActionCommand("stopBtn");
        //对播放录音进行注册监听
        playBtn = new JButton("播放录音");
        playBtn.addActionListener(this);
        playBtn.setActionCommand("playBtn");

        this.add(jp1, BorderLayout.NORTH);
        this.add(jp2, BorderLayout.CENTER);
        this.add(jp3, BorderLayout.SOUTH);

        jp2.setLayout(new GridLayout(1, 1, 50, 50));
        jp2.add(captureBtn);
        jp3.setLayout(new GridLayout(1, 2, 10, 10));
        jp3.add(stopBtn);
        jp3.add(playBtn);
        //设置按钮的属性
        captureBtn.setEnabled(true);

        JIntellitype.getInstance().registerHotKey(BEGIN_TALK, JIntellitype.MOD_ALT, (int) 'V');
        JIntellitype.getInstance().registerHotKey(END_TALK, JIntellitype.MOD_ALT, (int) 'S');
        JIntellitype.getInstance().addHotKeyListener(i -> {
            switch (i) {
                case BEGIN_TALK:
                    captureBtn.doClick();
                    break;
                case END_TALK:
                    stopBtn.doClick();
                    break;
            }
        });

        stopBtn.setEnabled(false);
        playBtn.setEnabled(false);
        //设置窗口的属性
        this.setSize(400, 300);
        this.setTitle("语音识别输入");
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        switch (e.getActionCommand()) {
            case "captureBtn":
                jLabel.setText("语音输入法");
                //点击开始录音按钮后的动作
                //停止按钮可以启动
                captureBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                playBtn.setEnabled(false);
                //调用录音的方法
                capture();
                break;
            case "stopBtn":
                //点击停止录音按钮的动作
                captureBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                playBtn.setEnabled(true);
                //调用停止录音的方法
                stop();
                new Thread(this::save).start();
                break;
            case "playBtn":
                //调用播放录音的方法
                play();
                break;
        }

    }

    //开始录音
    private void capture() {
        try {
            //af为AudioFormat也就是音频格式
            audioFormat = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) (AudioSystem.getLine(info));
            //打开具有指定格式的行，这样可使行获得所有所需的系统资源并变得可操作。
            targetDataLine.open(audioFormat);
            //允许某一数据行执行数据 I/O
            targetDataLine.start();

            //创建播放录音的线程
            Record record = new Record();
            Thread t1 = new Thread(record);
            t1.start();

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    //停止录音
    private void stop() {
        stopFlag = true;
        targetDataLine.stop();
    }

    //播放录音
    private void play() {
        byte audioData[] = outputStream.toByteArray();
        //转换为输入流
        inputStream = new ByteArrayInputStream(audioData);
        audioFormat = getAudioFormat();
        audioInputStream = new AudioInputStream(inputStream, audioFormat, audioData.length / audioFormat.getFrameSize());

        try {
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();
            //创建播放进程
            Play py = new Play();
            Thread t2 = new Thread(py);
            t2.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭流
                if (audioInputStream != null) {
                    audioInputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    //保存录音
    private void save() {
        jLabel.setText("保存中");
        //取得录音输入流
        audioFormat = getAudioFormat();

        byte audioData[] = outputStream.toByteArray();
        inputStream = new ByteArrayInputStream(audioData);
        audioInputStream = new AudioInputStream(inputStream, audioFormat, audioData.length / audioFormat.getFrameSize());
        //定义最终保存的文件名
        File file = null;
        //写入文件
        try {
            //以当前的时间命名录音的名字
            //将录音的文件存放到F盘下语音文件夹下
            File filePath = new File("audiofiles");
            if (!filePath.exists()) {//如果文件不存在，则创建该目录
                filePath.mkdir();
            }
            file = new File(filePath.getPath() + "/" + System.currentTimeMillis() + ".pcm");
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭流
            try {

                if (inputStream != null) {
                    inputStream.close();
                }
                if (audioInputStream != null) {
                    audioInputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        speechRecognise(file);
    }

    private void speechRecognise(File file) {
        captureBtn.setText("识别中");
        if (file == null) {
            return;
        }
        AsrMain asrMain = new AsrMain(file.toString());
        String result = "Error";
        try {
            result = asrMain.run();
        } catch (IOException | DemoException e) {
            e.printStackTrace();
        }
        captureBtn.setText("开始讲话");
        System.out.println(result);
        dealResult(result);
    }

    private void dealResult(String result) {
        JSONObject jsonObject = new JSONObject(result);
        if (jsonObject.getInt("err_no") != 0) {
            jLabel.setText("识别错误，请重说");
        } else {
            JSONArray strArr = jsonObject.getJSONArray("result");
            byte[] bytes = strArr.getString(0).getBytes();
            String strRec = null;
            try {
                strRec = new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //获取系统剪切板
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            //构建String数据类型
            StringSelection selection = new StringSelection(strRec);
            //添加文本到系统剪切板
            clipboard.setContents(selection, null);
            jLabel.setText("识别完成，粘贴可用");
        }
    }

    //设置AudioFormat的参数
    private AudioFormat getAudioFormat() {
        //下面注释部分是另外一种音频格式，两者都可以
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        float rate = 16000f;
        int sampleSize = 16;
        int channels = 1;
        return new AudioFormat(encoding, rate, sampleSize, channels,
                (sampleSize / 8) * channels, rate, true);
//		//采样率是每秒播放和录制的样本数
//		float sampleRate = 16000.0F;
//		// 采样率8000,11025,16000,22050,44100
//		//sampleSizeInBits表示每个具有此格式的声音样本中的位数
//		int sampleSizeInBits = 16;
//		// 8,16
//		int channels = 1;
//		// 单声道为1，立体声为2
//		boolean signed = true;
//		// true,false
//		boolean bigEndian = true;
//		// true,false
//		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,bigEndian);
    }

    //录音类，因为要用到MyRecord类中的变量，所以将其做成内部类
    class Record implements Runnable {
        //定义存放录音的字节数组,作为缓冲区
        byte bts[] = new byte[10000];

        //将字节数组包装到流里，最终存入到baos中
        //重写run函数
        @Override
        public void run() {
            outputStream = new ByteArrayOutputStream();
            try {
                System.out.println("ok3");
                stopFlag = false;
                while (!stopFlag) {
                    //当停止录音没按下时，该线程一直执行
                    //从数据行的输入缓冲区读取音频数据。
                    //要读取bts.length长度的字节,cnt 是实际读取的字节数
                    int cnt = targetDataLine.read(bts, 0, bts.length);
                    if (cnt > 0) {
                        outputStream.write(bts, 0, cnt);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    //关闭打开的字节数组流
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    targetDataLine.drain();
                    targetDataLine.close();
                }
            }
        }

    }

    //播放类,同样也做成内部类
    class Play implements Runnable {
        //播放baos中的数据即可
        @Override
        public void run() {
            byte bts[] = new byte[10000];
            try {
                int cnt;
                //读取数据到缓存数据
                while ((cnt = audioInputStream.read(bts, 0, bts.length)) != -1) {
                    if (cnt > 0) {
                        //写入缓存数据
                        //将音频数据写入到混频器
                        sourceDataLine.write(bts, 0, cnt);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                sourceDataLine.drain();
                sourceDataLine.close();
            }


        }
    }
}