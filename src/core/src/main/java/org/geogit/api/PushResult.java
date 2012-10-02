/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

public class PushResult {

    private STATUS status;

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public static enum STATUS {
        OK_APPLIED {
            @Override
            public int value() {
                return 0;
            }
        },
        CONFLICT {
            @Override
            public int value() {
                return 1;
            }
        }, INCORRECT_PARAMETER {
            @Override
            public int value() {
                return 1;
            }
        } ,
        NO_CHANGE {
            @Override
            public int value() {
                return 1;
            }
        };

        public abstract int value();

        public static STATUS valueOf(final int value) {
            return STATUS.values()[value];
        }
    }
}
