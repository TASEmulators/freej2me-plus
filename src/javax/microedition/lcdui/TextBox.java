/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.lcdui;

import org.recompile.mobile.Mobile;

public class TextBox extends Screen
{

	private String text = "";
	private int max;
	private int constraints;
	private String mode;
	private int caretPosition;
	private int padding;
	private int margin;

	private char[][] charSet = // Not all of these charsets are complete.
	{
		"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%~^<.,;?/[]{}>&*()-_+'\"|`\n".toCharArray(), // Default subset, BASIC_LATIN, IS_LATIN
		"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%~^<.,;?/[]{}>&*()-_+'\"|`\n".toCharArray(),                           // MIDP_UPPERCASE_LATIN
		"abcdefghijklmnopqrstuvwxyz!@#$%~^<.,;?/[]{}>&*()-_+'\"|`0123456789\n".toCharArray(),                           // MIDP_LOWERCASE_LATIN
		"0123456789".toCharArray(),                                                                                     // NUMERIC, IS_LATIN_DIGITS
		"αβγδεζηθικλμνξοπρστυφχψω\n".toCharArray(),                                                                      // UCB_GREEK
		"абвгдежзийклмнопрстуфхцчшщъыьэюя\n".toCharArray(),                                                              // UCB_CYRILLIC
		"աբգդեիզլւխճմյնոպջռտուքֆք\n".toCharArray(),                                                                    // UCB_ARMENIAN
		"אבגדהווזחטיך\n".toCharArray(),                                                                                     // UCB_HEBREW
		"ابجدهوزحطی\n".toCharArray(),                                                                                     // UCB_ARABIC
		"अआइईउऊऋएऐओऔकखगघचछजझटठडढणतथदधनपरयलवशषसह\n".toCharArray(),                                                     // UCB_DEVANAGARI
		"অআইঈউঊএঐও\n".toCharArray(),                                                                                  // UCB_BENGALI
		"กขฃคฅฆงจฉชซฌญฎฏฐฑฒณดตถทธนบปผฝพฟห\n".toCharArray(),                                                            // UCB_THAI
		"あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん\n".toCharArray(),                 // UCB_HIRAGANA
		"アイウエオカキクケコサシスセソタチツテトナニヌネノ\n".toCharArray(),                                                    // UCB_KATAKANA
		"가각갂갃간갅갆갇\n".toCharArray(),                                                                                 // USB_HANGUL_SYLLABLES
		"０１２３４５６７８９\n".toCharArray(),                                                                             // IS_FULLWIDTH_DIGITS
		"ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ\n".toCharArray(),       // IS_FULLWIDTH_LATIN
		"ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ\n".toCharArray(),                                                      // IS_HALFWIDTH_KATAKANA
		"日月火水木金土山川田口目耳手足心大小多少新古白黒青赤\n".toCharArray(),                                                  // IS_HANJA, IS_KANJI
		"我你他她它是不在有这那了人们说去来好学吃喝玩笑爱天日月年时\n".toCharArray(),                                             // IS_SIMPLIFIED_HANZI
		"我你他她它是不好在有這那了人們說去來好學吃喝玩笑愛天日月年時\n".toCharArray()                                            // IS_TRADITIONAL_HANZI
	};

	private char[][] charSetHint =
	{
		"Lat".toCharArray(),
		"LAT".toCharArray(),
		"lat".toCharArray(),
		"NUM".toCharArray(),
		"GRK".toCharArray(),
		"CYR".toCharArray(),
		"ARME".toCharArray(),
		"HEBR".toCharArray(),
		"ARAB".toCharArray(),
		"DEVA".toCharArray(),
		"BENG".toCharArray(),
		"THAI".toCharArray(),
		"JP_あ".toCharArray(),
		"JP_ア".toCharArray(),
		"HANG".toCharArray(),
		"F_NUM".toCharArray(),
		"F_LAT".toCharArray(),
		"JP_ｱ".toCharArray(),
		"TH_日".toCharArray(),
		"JP_日".toCharArray(),
		"CN_S".toCharArray(),
		"CN_T".toCharArray()
	};

	private byte charSetIdx = 0; // Maps to the charsets above

	private int selectedCharIndex = 0;  // Index for the currently selected character


	public TextBox(String Title, String value, int maxSize, int Constraints)
	{
		title = Title;
		setMaxSize(maxSize);
		setConstraints(Constraints);
		setString(value);

		caretPosition = text.length();

		// these can't be static because of Font.getDefaultFont().getHeight()
		padding = Font.getDefaultFont().getHeight() / 3;
		margin = Font.getDefaultFont().getHeight() / 5;
	}

	public void delete(int offset, int length)
	{
		text = text.substring(0, offset) + text.substring(offset+length);
		if (caretPosition > text.length()) {
			caretPosition = text.length();
		}
		_invalidate();
	}

	public int getCaretPosition() { return caretPosition; }

	public int getChars(char[] data)
	{
		for(int i=0; i<text.length(); i++)
		{
			data[i] = text.charAt(i);
		}
		return text.length();
	}

	public int getConstraints() { return constraints; }

	public int getMaxSize() { return max; }

	public String getString() { return text; }

	public void insert(char[] data, int offset, int length, int position)
	{
		if(data == null) { throw new NullPointerException("String cannot be null"); }
		if(text.length() + length > max) { throw new IllegalArgumentException("Resulting string would be too long"); }
		if (offset < 0 || length < 0 || offset > data.length || (offset + length) > data.length)
		{
			throw new ArrayIndexOutOfBoundsException("Invalid offset or length for data array");
		}

		StringBuilder out = new StringBuilder();
		out.append(text, 0, position);
		out.append(data, offset, length);
		out.append(text.substring(position));
		String res = out.toString();

		int baseConstraint = constraints & TextField.CONSTRAINT_MASK;
		if (!TextField.isTextValidForConstraint(res, baseConstraint))
		{
			throw new IllegalArgumentException("Resulting string is illegal for current input constraints");
		}

		text = res;
		caretPosition = text.length();

		_invalidate();
	}

	public void insert(String src, int position)
	{
		if(src == null) { throw new NullPointerException("String cannot be null"); }
		if(text.length() + src.length() > max) { throw new IllegalArgumentException("Resulting string would be too long"); }

		StringBuilder out = new StringBuilder();
		out.append(text, 0, position);
		out.append(src);
		out.append(text.substring(position));
		text = out.toString();

		caretPosition = text.length();

		_invalidate();
	}

	public void setChars(char[] data, int offset, int length)
	{
		StringBuilder out = new StringBuilder();
		out.append(data, offset, length);
		text = out.toString();
		caretPosition = text.length();
		_invalidate();
	}

	public void setConstraints(int Constraints)
	{
		int restrictiveConst = Constraints & TextField.CONSTRAINT_MASK;

		boolean isValidBase = (restrictiveConst == TextField.ANY ||
			restrictiveConst == TextField.EMAILADDR ||
			restrictiveConst == TextField.NUMERIC ||
			restrictiveConst == TextField.PHONENUMBER ||
			restrictiveConst == TextField.URL ||
			restrictiveConst == TextField.DECIMAL);

		if (!isValidBase) { throw new IllegalArgumentException("Invalid base constraint."); }

		int modifierFlags = TextField.PASSWORD | TextField.UNEDITABLE | TextField.SENSITIVE |
			TextField.NON_PREDICTIVE | TextField.INITIAL_CAPS_WORD |  TextField.INITIAL_CAPS_SENTENCE;
		int unknownBits = Constraints & ~(TextField.CONSTRAINT_MASK | modifierFlags);
		if (unknownBits != 0) { throw new IllegalArgumentException("Unknown constraint modifier flags."); }

		this.constraints = Constraints;
		_invalidate();
	}

	public void setInputMode(int mode) { charSetIdx = (byte) mode; }

	public void setInitialInputMode(String characterSubset)
	{
		if(characterSubset == null)
		{
			mode = "MIDP_UPPERCASE_LATIN";
			charSetIdx = 1;
			return;
		}

		mode = characterSubset;

		if (mode.equals("MIDP_UPPERCASE_LATIN"))                           { charSetIdx = 1; }
		else if (mode.equals("MIDP_LOWERCASE_LATIN"))                      { charSetIdx = 2; }
		else if (mode.equals("NUMERIC") || mode.equals("IS_LATIN_DIGITS")) { charSetIdx = 3; }
		else if (mode.equals("UCB_GREEK"))                                 { charSetIdx = 4; }
		else if (mode.equals("UCB_CYRILLIC"))                              { charSetIdx = 5; }
		else if (mode.equals("UCB_ARMENIAN"))                              { charSetIdx = 6; }
		else if (mode.equals("UCB_HEBREW"))                                { charSetIdx = 7; }
		else if (mode.equals("UCB_ARABIC"))                                { charSetIdx = 8; }
		else if (mode.equals("UCB_DEVANAGARI"))                            { charSetIdx = 9; }
		else if (mode.equals("UCB_BENGALI"))                               { charSetIdx = 10; }
		else if (mode.equals("UCB_THAI"))                                  { charSetIdx = 11; }
		else if (mode.equals("UCB_HIRAGANA"))                              { charSetIdx = 12; }
		else if (mode.equals("UCB_KATAKANA"))                              { charSetIdx = 13; }
		else if (mode.equals("USB_HANGUL_SYLLABLES"))                      { charSetIdx = 14; }
		else if (mode.equals("IS_FULLWIDTH_DIGITS"))                       { charSetIdx = 15; }
		else if (mode.equals("IS_FULLWIDTH_LATIN"))                        { charSetIdx = 16; }
		else if (mode.equals("IS_HALFWIDTH_KATAKANA"))                     { charSetIdx = 17; }
		else if (mode.equals("IS_HANJA"))                                  { charSetIdx = 18; }
		else if (mode.equals("IS_SIMPLIFIED_HANZI"))                       { charSetIdx = 19; }
		else if (mode.equals("IS_TRADITIONAL_HANZI"))                      { charSetIdx = 20; }
		else                                                               { charSetIdx = 0; } // Default subset (BASIC_LATIN, IS_LATIN)
	}

	public int setMaxSize(int maxSize)
	{
		if(maxSize <= 0) { throw new IllegalArgumentException("Invalid max size"); }
		max = maxSize;
		if(text.length() > maxSize) { text = text.substring(0, max); }
		return max;
	}

	public void setString(String value)
	{
		if (value == null) { value = ""; }
		if(value.length() > max) { throw new IllegalArgumentException("String is too long"); }

		int baseConstraint = constraints & TextField.CONSTRAINT_MASK;
		if (!TextField.isTextValidForConstraint(value, baseConstraint))
		{
			throw new IllegalArgumentException("String is illegal for the current input constraints");
		}

		text = value;
		caretPosition = text.length();
		_invalidate();
	}

	public void setTicker(Ticker tick) { ticker = tick; }

	public void setTitle(String s) { title = s; }

	public int size() { return text.length(); }

	public boolean screenKeyPressed(int key)
	{
		boolean handled = true;

		if(constraints == TextField.UNEDITABLE) { return false; } // If this field is uneditable, the user shall not be able to make changes through input
		else
		{
			if (key == Canvas.DOWN) { selectedCharIndex = (selectedCharIndex - 1 + charSet[charSetIdx].length) % charSet[charSetIdx].length; } // Cycle down through the character set
			else if (key == Canvas.UP) { selectedCharIndex = (selectedCharIndex + 1) % charSet[charSetIdx].length; } // Cycle up through the character set
			else if (key == Canvas.LEFT && caretPosition > 0) // Move back one char
			{
				caretPosition--;
				// Check the character under the caret
				char currentChar = text.charAt(caretPosition);
				// Find the index of the current character in charSet
				for (int i = 0; i < charSet[charSetIdx].length; i++)
				{
					if (charSet[charSetIdx][i] == currentChar)
					{
						selectedCharIndex = i;
						break;
					}
				}
			}
			else if (key == Canvas.RIGHT && caretPosition < text.length()) // Move forward one char
			{
				if(caretPosition+1 < text.length())
				{
					char currentChar = text.charAt(caretPosition+1);
					for (int i = 0; i < charSet[charSetIdx].length; i++)
					{
						if (charSet[charSetIdx][i] == currentChar)
						{
							selectedCharIndex = i;
							break;
						}
					}
				}
				caretPosition++;
			}
			else if (key == Canvas.FIRE || key == Canvas.KEY_NUM5) // Insert the selected character into the current caret position
			{
				if (caretPosition < text.length()) // Replace the character at the caret position
				{
					text = text.substring(0, caretPosition) + charSet[charSetIdx][selectedCharIndex] + text.substring(caretPosition + 1);
					caretPosition++;
				}
				else // Append if at the end if the caret is already at the end
				{
					if(text.length() < max)
					{
						text += charSet[charSetIdx][selectedCharIndex];
						caretPosition++;
					}
				}
			}
			else if (key == Canvas.KEY_STAR) // Remove the char at the current caret position
			{
				if (caretPosition < text.length())
				{
					// Remove the character at the caret position
					text = text.substring(0, caretPosition) + text.substring(caretPosition + 1);
					// Optionally, move caret left after deletion
					caretPosition = Math.max(caretPosition - 1, 0);
				}
			}
			else if (key == Canvas.KEY_POUND && constraints != (TextField.NUMERIC | TextField.EMAILADDR | TextField.PHONENUMBER | TextField.DECIMAL)) // Insert a space into the current caret position (in constrants that allow it)
			{
				if (caretPosition < text.length() && text.length() < max) // Replace the character at the caret position
				{
					text = text.substring(0, caretPosition) + ' ' + text.substring(caretPosition);
					caretPosition++;
				}
				else // Append if at the end if the caret is already at the end
				{
					if(text.length() < max)
					{
						text += ' ';
						caretPosition++;
					}
				}
			}
			else { handled = false; }

			if (handled) { _invalidate(); }
			return handled;
		}
	}

	protected String renderScreen(int x, int y, int width, int height)
	{
		LCDUIRenderer.drawTextField(graphics, x, y, width, height,
			text, caretPosition, charSet, charSetIdx, selectedCharIndex,
			charSetHint, max, margin, padding, true, true);
		return null;
	}
 }
