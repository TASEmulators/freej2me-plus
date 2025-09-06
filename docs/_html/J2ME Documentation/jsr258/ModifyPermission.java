package javax.microedition.theme;

import java.security.BasicPermission;

/**
 * This class represents access rights to modify skins and themes in
 * Mobile UI Customization API.
 * A <code>ModifyPermission</code> contains a name (also referred to as a
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
 * <td>Theme#updateElement(String, String)
 * <br>Theme#updateToolkitElement(String, String, String)
 * <br>Skin#updateApplicationElement(String, String)
 * <br>Theme#commit()
 * </td>
 * <td>modify</td>
 * </tr>
 *
 * </table>
 *
 * <p>As defined for <code>BasicPermission</code> the naming follows the
 * hierarchical property naming convention. An asterisk may appear by itself,
 * or if immediately preceded by a "." may appear at the end of the name,
 * to signify a wildcard match.</p>
 *
 * @see			<a href="http://www.jcp.org/en/jsr/detail?id=258">
 *              Mobile UI Customization API</a>
 */
public final class ModifyPermission extends BasicPermission {
	/**
	 * Creates a new <code>ModifyPermission</code> instance with the
     * specified name. The name string should conform to the specification
     * given above.
     *
	 * @param	name of the <code>ModifyPermission</code>.
     * @throws		NullPointerException if <code>name</code> is <code>null</code>.
     * @throws		IllegalArgumentException if <code>name</code> is empty.
	 */
	public ModifyPermission(String name){
        super(name);
    }
}
