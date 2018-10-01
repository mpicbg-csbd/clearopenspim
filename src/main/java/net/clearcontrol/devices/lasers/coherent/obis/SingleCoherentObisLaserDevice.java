package net.clearcontrol.devices.lasers.coherent.obis;

import clearcontrol.com.serial.Serial;
import clearcontrol.com.serial.SerialDevice;
import clearcontrol.core.variable.Variable;
import clearcontrol.core.variable.VariableSetListener;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.devices.lasers.LaserDeviceBase;
import clearcontrol.devices.lasers.LaserDeviceInterface;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * SingleCoherentObisLaserDevice
 *
 * Inspired by code from Forrest Collman  MBL, Woods Hole, MA 2014
 *                Karl Hoover University of California, San Francisco, 2009 (Hoover)
 * https://valelab4.ucsf.edu/svn/micromanager2/trunk/DeviceAdapters/CoherentOBIS/CoherentOBIS.cpp
 *
 *
 * TODO: implement serial, usage hours and wavelength read/write
 *
 * Author: @haesleinhuepf
 * 09 2018
 */
public class SingleCoherentObisLaserDevice extends LaserDeviceBase implements LaserDeviceInterface {
    final static String POWER_PROPERTY_TOKEN = ("SOUR1:POW:LEV:IMM:AMPL");

    final static String LASER_ON_TOKEN = ("SOUR1:AM:STATE");

    final static String SERIAL_TOKEN = ("SYST:INF:SNUM");
    final static String USAGE_HOURS_TOKEN = ("SYST1:DIOD:HOUR");
    final static String WAVELENGTH_TOKEN = ("SYST1:INF:WAV");
    final static String EXTERNAL_POWER_CONTROL_TOKEN = ("SOUR1:POW:LEV:IMM:AMPL");
    final static String MAX_POWER_TOKEN = ("SOUR1:POW:LIM:HIGH");
    final static String MIN_POWER_TOKEN = ("SOUR1:POW:LIM:LOW");

    //private SerialDevice mSerialDevice;
    private boolean connected = false;

    public SingleCoherentObisLaserDevice(String serialPort, int baudRate, int wavelength) {
        super("Coherent OBIS laser " + wavelength + " on " + serialPort);

        super.mLaserOnVariable = new Variable<Boolean>("Laser on", false);
        mLaserOnVariable.addSetListener(new VariableSetListener<Boolean>() {
            @Override
            public void setEvent(Boolean pCurrentValue, Boolean pNewValue) {
                turnLaser(pNewValue);
            }
        });

        super.mPowerOnVariable = new Variable<Boolean>("Power on", false);

        super.mTargetPowerInMilliWattVariable = new BoundedVariable<Number>("Power", 0.0,0.0, Double.MAX_VALUE);
        mTargetPowerInMilliWattVariable.addSetListener(new VariableSetListener<Number>() {
            @Override
            public void setEvent(Number pCurrentValue, Number pNewValue) {
                setLaserPower(pNewValue.doubleValue());
            }
        });

        super.mWavelengthVariable = new BoundedVariable<Integer>("Wavelength in nm", wavelength, 0, Integer.MAX_VALUE);
        //mSerialDevice = new SerialDevice("Serial laser " + getName(), serialPort, baudRate);
    }

    @Override
    public boolean start() {
        return connect();
    }

    @Override
    public boolean stop() {
        return disconnect();
    }

    // ---------------------------------------------
    // high level API

    private boolean connect() {
        //mSerialDevice.getSerial().setLineTerminationCharacter('\n');
        //mSerialDevice.open();
        purge();

        setLaserProperty("SYST1:COMM:HAND","On");
        setLaserProperty("SYST1:COMM:PROM","Off");

        send("SYST1:ERR:CLE");

        connected = true;

        return connected;
    }

    private void purge() {
        /*Serial pSerial = mSerialDevice.getSerial();
        pSerial.setMessageLength(100);
        pSerial.readBinaryMessage(100);
        try {
            pSerial.purge();
        } catch (SerialPortException e) {
            e.printStackTrace();
        }*/
    }

    private boolean disconnect() {
        if (!connected) {
            return false;
        }
        turnLaser(false);
        //mSerialDevice.close();
        return true;
    }

    private boolean setLaserPower(double value) {
        if (!connected) {
            return false;
        }

        String powert = String.format("%4.6f", value);
        String result = setLaserProperty(POWER_PROPERTY_TOKEN, powert);
        return powert.compareTo(result) == 0;

    }

    private boolean turnLaser(boolean on) {
        if (!connected) {
            return false;
        }

        String state = on?"On":"Off";
        String result = setLaserProperty(LASER_ON_TOKEN, state);
        return state.compareTo(result) == 0;
    }

    // --------------------------------------------
    // low level API

    private String send(String message) {
        SerialPort serialPort = new SerialPort("COM3");
        try {
            serialPort.openPort();//Open serial port
            serialPort.setParams(SerialPort.BAUDRATE_115200,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);//Set params. Also you can set params by this string: serialPort.setParams(9600, 8, 1, 0);
            serialPort.writeBytes((message + "\r").getBytes());//Write data to port
            System.out.print(message.replace('\r', ' ').trim() + " --> ");

            StringBuilder b = new StringBuilder();
            byte last;
            do {
                String ret = serialPort.readString(1);
                b.append(ret);
                last = ret.getBytes()[0];
            } while(last != '\n');
            System.out.println(b.toString().trim());
            serialPort.closePort();//Close serial port
            return b.toString();
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
            return null;
        }
        /*
        try
        {
            System.out.print(message.replace('\r', ' ').trim() + " --> ");
            String lAnswer =
                    mSerialDevice.getSerial()
                            .writeStringAndGetAnswer(message + "\n");
            System.out.println(lAnswer.trim());
            return lAnswer;
        }
        catch (SerialPortException e)
        {
            e.printStackTrace();
            return null;
        }*/
    }

    private String setLaserProperty(String property, String value) {
        send(property + " " + value);
        return getLaserProperty(property);
    }

    private String getLaserProperty(String property) {
        return send(property + "?");
    }
}
