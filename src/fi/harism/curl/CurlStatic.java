/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.curl;

public final class CurlStatic {

	// Shaders.
	
	public static final String SHADER_SHADOW_FRAGMENT = ""
			+ "precision mediump float;                                       \n"
			+ "varying vec4 vColor;                                           \n"
			+ "void main() {                                                  \n"
			+ "  gl_FragColor = vColor;                                       \n"
			+ "}                                                              \n";
	
	public static final String SHADER_SHADOW_VERTEX = ""
			+ "uniform mat4 uProjectionM;                                     \n"
			+ "attribute vec3 aPosition;                                      \n"
			+ "attribute vec4 aColor;                                         \n"
			+ "varying vec4 vColor;                                           \n"
			+ "void main() {                                                  \n"
			+ "  gl_Position = uProjectionM * vec4(aPosition, 1.0);           \n"
			+ "  vColor = aColor;                                             \n"
			+ "}                                                              \n";
	
	public static final String SHADER_TEXTURE_FRAGMENT = ""
			+ "precision mediump float;                                       \n"
			+ "varying vec4 vColor;                                           \n"
			+ "varying vec2 vTextureCoord;                                    \n"
			+ "uniform sampler2D sTexture;                                    \n"
			+ "void main() {                                                  \n"
			+ "  gl_FragColor = texture2D(sTexture, vTextureCoord);           \n"
			+ "  gl_FragColor.rgb *= vColor.rgb;                              \n"
			+ "  gl_FragColor = mix(vColor, gl_FragColor, vColor.a);          \n"
			+ "  gl_FragColor.a = 1.0;                                        \n"
			+ "}                                                              \n";
	
	public static final String SHADER_TEXTURE_VERTEX = ""
			+ "uniform mat4 uProjectionM;                                     \n"
			+ "attribute vec3 aPosition;                                      \n"
			+ "attribute vec4 aColor;                                         \n"
			+ "attribute vec2 aTextureCoord;                                  \n"
			+ "varying vec4 vColor;                                           \n"
			+ "varying vec2 vTextureCoord;                                    \n"
			+ "void main() {                                                  \n"
			+ "  gl_Position = uProjectionM * vec4(aPosition, 1.0);           \n"
			+ "  vColor = aColor;                                             \n"
			+ "  vTextureCoord = aTextureCoord;                               \n"
			+ "}                                                              \n";
	
}
