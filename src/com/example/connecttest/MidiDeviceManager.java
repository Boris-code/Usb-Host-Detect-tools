package com.example.connecttest;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.util.UsbMidiDriver;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.widget.Toast;
import android.util.Log;

public class MidiDeviceManager {
    private static final String TAG = "MidiDeviceManager";
    private UsbMidiDriver mUsbMidiDriver = null;
    private boolean mIsConnected;
    private static MidiDeviceManager sInstance = null;

    private static final boolean DEBUG = false;

    public boolean isConnected() {
        return mIsConnected;
    }

    public static MidiDeviceManager getInstance() {
        if (sInstance == null) {
            sInstance = new MidiDeviceManager();
        }

        return sInstance;
    }

    public void init(final Context context) {
        mIsConnected = false;

        mUsbMidiDriver = new UsbMidiDriver(context) {
            @Override
            public void onDeviceAttached(UsbDevice usbDevice) {
                mIsConnected = true;
                Toast.makeText(context, "该设备支持USB host", Toast.LENGTH_LONG).show();
                if (DEBUG) {
                    Toast.makeText(context, "USB MIDI Device " + usbDevice.getDeviceName() + " has been attached.", Toast.LENGTH_LONG).show();
                }
                onAttached();
            }

            @Override
            public void onDeviceDetached(UsbDevice usbDevice) {
                mIsConnected = false;
                if (DEBUG) {
                    Toast.makeText(context, "USB MIDI Device " + usbDevice.getDeviceName() + " has been detached.", Toast.LENGTH_LONG).show();
                }
                onDetached();
            }

            @Override
            public void onMidiNoteOn(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
                onMidiNote(channel, note, velocity);
            }

            @Override
            public void onMidiNoteOff(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
                onMidiNote(channel, note, velocity);
            }

            @Override
            public void onMidiSystemExclusive(MidiInputDevice sender, int cable, final byte[] systemExclusive) {
                onMidiSysEx(systemExclusive);
            }

            @Override
            public void onMidiSystemCommonMessage(MidiInputDevice sender, int cable, byte[] bytes) {
            }

            @Override
            public void onMidiSingleByte(MidiInputDevice sender, int cable, int byte1) {
            }

            @Override
            public void onMidiProgramChange(MidiInputDevice sender, int cable, int channel, int program) {
            }

            @Override
            public void onMidiPolyphonicAftertouch(MidiInputDevice sender, int cable, int channel, int note, int pressure) {
            }

            @Override
            public void onMidiPitchWheel(MidiInputDevice sender, int cable, int channel, int amount) {
            }

            @Override
            public void onMidiMiscellaneousFunctionCodes(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
            }

            @Override
            public void onMidiControlChange(MidiInputDevice sender, int cable, int channel, int function, int value) {
            }

            @Override
            public void onMidiChannelAftertouch(MidiInputDevice sender, int cable, int channel, int pressure) {
            }

            @Override
            public void onMidiCableEvents(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
            }
        };

        open();
    }

    public void open() {
        mUsbMidiDriver.open();
    }

    public void close() {
        mUsbMidiDriver.close();
    }

    public void sendMidiNote(int channel, int note, int velocity) {
        if (mUsbMidiDriver != null) {
            for (UsbDevice usbDevice : mUsbMidiDriver.getConnectedUsbDevices()) {
                for (MidiOutputDevice midiOutputDevice : mUsbMidiDriver.getMidiOutputDevices(usbDevice)) {
                    if (velocity > 0 && velocity < 128) {
                        midiOutputDevice.sendMidiNoteOn(0, channel, note, velocity);
                    } else if (velocity == 0) {
                        midiOutputDevice.sendMidiNoteOff(0, channel, note, velocity);
                    }
                }
            }
        }
    }

    // JNI call from native
    // light: 0, 1, 2, 3
    public void sendMidiLight(int note, int light) {
        if (mUsbMidiDriver != null) {
            for (UsbDevice usbDevice : mUsbMidiDriver.getConnectedUsbDevices()) {
                for (MidiOutputDevice midiOutputDevice : mUsbMidiDriver.getMidiOutputDevices(usbDevice)) {
                    midiOutputDevice.sendMidiPolyphonicAftertouch(0, 2, note - 21, light);
                }
            }
        }
    }

    // JNI call from native
    public void sendMidiPedal(boolean on) {
        if (mUsbMidiDriver != null) {
            for (UsbDevice usbDevice : mUsbMidiDriver.getConnectedUsbDevices()) {
                for (MidiOutputDevice midiOutputDevice : mUsbMidiDriver.getMidiOutputDevices(usbDevice)) {
                    midiOutputDevice.sendMidiControlChange(0, 0, 0x40, on ? 0x7f : 0x00);
                }
            }
        }
    }

    // JNI call from native
    public void sendMidiSysEx(byte[] data) {
        if (mUsbMidiDriver != null) {
            for (UsbDevice usbDevice : mUsbMidiDriver.getConnectedUsbDevices()) {
                for (MidiOutputDevice midiOutputDevice : mUsbMidiDriver.getMidiOutputDevices(usbDevice)) {
                    midiOutputDevice.sendMidiSystemExclusive(0, data);
                }
            }
        }
    }

    public void turnOffAllLights() {
        final byte data[] = { (byte) 0xf0, (byte) 0x00, (byte) 0x20, (byte) 0x2b, (byte) 0x69, (byte) 0x16, (byte) 0x02, (byte) 0x00, (byte) 0xf7 };
        sendMidiSysEx(data);
    }

    public void connectDevice() {
        final byte data[] = { (byte) 0xf0, (byte) 0x00, (byte) 0x20, (byte) 0x2b, (byte) 0x69, (byte) 0x00, (byte) 0x00, (byte) 0x55, (byte) 0x79, (byte) 0xf7 };
        sendMidiSysEx(data);
    }

    public void queryDeviceInfo() {
        byte[] query = { (byte) 0xf0, 0x00, 0x20, 0x2b, 0x69, 0x01, 0x00, 0x00, (byte) 0xf7 };
        sendMidiSysEx(query);
    }

    public void setSwitchPianoSound(boolean isOn) {
        byte[] query = { (byte) 0xf0, 0x00, 0x20, 0x2b, 0x69, 0x17, 0x00, isOn ? (byte)0x01 : (byte)0x00, (byte) 0xf7 };
        sendMidiSysEx(query);
    }

    public void setAutoShutdownTime(int timeCode) {
        byte[] query = { (byte) 0xf0, 0x00, 0x20, 0x2b, 0x69, 0x15, 0x00, (byte)timeCode, (byte) 0xf7 };
        sendMidiSysEx(query);
    }

    public void onAttached(){
    	MainActivity.getInstance().supportUsbHost();
    }

    public void onDetached(){}

    public void onMidiNote(int channel, int note, int velocity){};

    public void onMidiSysEx(final byte[] systemExclusive){}
}