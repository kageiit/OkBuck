/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.okbuck.example.javalib;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class DummyJavaClass {
    public String getJavaWord() {
        final String mock = "{\"lang\":\"Java\"}";
        final String mock2 = "Mock string from DummyJavaClass";
        new Thread(() -> System.out.println(mock2 + " 1")).start();
        dummyCall(System.out::println, mock2 + " 2");
        return new GsonBuilder().create().fromJson(mock, DummyObject.class).lang;
    }

    private void dummyCall(DummyInterface dummyInterface, String val) {
        dummyInterface.call(val);
    }

    private static class DummyObject {
        @SerializedName("lang")
        private String lang;
    }

    public interface DummyInterface {
        void call(String v);
    }
}
