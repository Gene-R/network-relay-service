/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

/**
 *
 * @author eugener
 */
public enum RelayPolicy {
    FIRST_ON_SUCCESS, ROUND_ROBIN, ORA_TRANSFORM, ACTIVATE_IF_STOPPED, NONE;

    public static RelayPolicy fromString(String strName) throws IllegalArgumentException {
        for (RelayPolicy bp : values()) {
            if (bp.toString().toUpperCase().equals(strName.toUpperCase())) {
                return bp;
            }
        }
        throw new IllegalArgumentException("No such policy: " + strName);
    }
}
