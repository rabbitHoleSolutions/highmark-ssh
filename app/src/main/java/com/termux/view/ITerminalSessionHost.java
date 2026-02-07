package com.termux.view;

import com.termux.terminal.TerminalEmulator;

/**
 * Interface that abstracts the terminal session for {@link TerminalView}.
 * This replaces the direct dependency on {@link com.termux.terminal.TerminalSession}
 * to allow plugging in SSH-backed sessions instead of local process sessions.
 */
public interface ITerminalSessionHost {

    /** Get the terminal emulator for rendering. */
    TerminalEmulator getEmulator();

    /** Write a UTF-8 string to the session. */
    void write(String data);

    /** Write raw bytes to the session. */
    void write(byte[] data, int offset, int count);

    /** Write a unicode code point, optionally prepending ESC. */
    void writeCodePoint(boolean prependEscape, int codePoint);

    /** Update the terminal size. */
    void updateSize(int columns, int rows);

    /** Get the terminal title. */
    String getTitle();

    /** Reset the terminal emulator state. */
    void reset();

    /** Copy text to the clipboard. */
    void onCopyTextToClipboard(String text);

    /** Paste text from the clipboard. */
    void onPasteTextFromClipboard();
}
