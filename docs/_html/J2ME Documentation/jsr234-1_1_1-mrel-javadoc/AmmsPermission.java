package javax.microedition.amms;

import java.security.BasicPermission;


/**
 * This class represents access rights to Advanced Multimedia Supplements API.
 * An <code>AmmsPermission</code> contains a name (also referred to as a
 * "target name") but no actions list; you either have the named permission
 * or you don't.
 *
 * <p>The protected API calls and corresponding target names are:
 *
 * <table border=1 cellpadding=5 summary="API call, permission target name">
 *
 * <tr>
 * <th>API call checking the permission</th>
 * <th>Permission target name</th>
 * </tr>
 *
 * <tr>
 * <td>CameraControl.enableShutterFeedback()</td>
 * <td>cameraControl.enableShutterFeedback</td>
 * </tr>
 *
 * <tr>
 * <td>TunerControl.setPreset(int preset)
 * <br>TunerControl.setPreset(int preset, int freq, java.lang.String mod, int stereomode)
 * <br>TunerControl.setPresetName(int preset, java.lang.String name)
 * </td>
 * <td>tunerControl.setPreset</td>
 * </tr>
 *
 * </table>
 *
 * <p>As defined for <code>BasicPermission</code> the naming follows the
 * hierarchical property naming convention. An asterisk may appear by itself,
 * or if immediately preceded by a "." may appear at the end of the name,
 * to signify a wildcard match.</p>
 *
 * <p>The action string is unused.
 *
 * @see			<a href="http://www.jcp.org/en/jsr/detail?id=234">
 *              Advanced Multimedia Supplements API</a>
 */
public final class AmmsPermission extends BasicPermission {

	/**
	 * Creates a new <code>AmmsPermission</code> instance with the
     * specified name. The name string should conform to the specification
     * given above.
     *
	 * @param	name of the <code>AmmsPermission</code>.
     * @throws		NullPointerException if <code>name</code> is <code>null</code>.
     * @throws		IllegalArgumentException if <code>name</code> is empty.
	 */
	public AmmsPermission(String name){
        super(name);
    }
}
