/*
   Copyright 2013 Harri Smatt

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
			+ "varying vec2 vPenumbra;                                        \n"
			+ "void main() {                                                  \n"
			+ "  float alpha = 1.0 - length(vPenumbra);                       \n"
			+ "  alpha = smoothstep(0.0, 2.0, alpha);                         \n"
			+ "  gl_FragColor = vec4(0.0, 0.0, 0.0, alpha);                   \n"
			+ "}                                                              \n";

	public static final String SHADER_SHADOW_VERTEX = ""
			+ "uniform mat4 uProjectionM;                                     \n"
			+ "attribute vec3 aPosition;                                      \n"
			+ "attribute vec2 aPenumbra;                                      \n"
			+ "varying vec2 vPenumbra;                                        \n"
			+ "void main() {                                                  \n"
			+ "  vec3 pos = vec3(aPosition.xy + aPenumbra, aPosition.z);      \n"
			+ "  gl_Position = uProjectionM * vec4(pos, 1.0);                 \n"
			+ "  vPenumbra = normalize(aPenumbra);                            \n"
			+ "}                                                              \n";

	public static final String SHADER_TEXTURE_FRAGMENT = ""
			+ "precision mediump float;                                       \n"
			+ "uniform sampler2D sTextureFront;                               \n"
			+ "uniform sampler2D sTextureBack;                                \n"
			+ "uniform vec4 uColorFront;                                      \n"
			+ "uniform vec4 uColorBack;                                       \n"
			+ "varying vec3 vNormal;                                          \n"
			+ "varying vec2 vTexCoord;                                        \n"
			+ "void main() {                                                  \n"
			+ "  vec3 normal = normalize(vNormal);                            \n"
			+ "  if (normal.z >= 0.0) {                                       \n"
			+ "    gl_FragColor = texture2D(sTextureFront, vTexCoord);        \n"
			+ "    gl_FragColor.rgb = mix(gl_FragColor.rgb,                   \n"
			+ "                   uColorFront.rgb, uColorFront.a);            \n"
			+ "  }                                                            \n"
			+ "  else {                                                       \n"
			+ "    gl_FragColor = texture2D(sTextureBack, vTexCoord);         \n"
			+ "    gl_FragColor.rgb = mix(gl_FragColor.rgb,                   \n"
			+ "                   uColorBack.rgb, uColorBack.a);              \n"
			+ "  }                                                            \n"
			+ "  gl_FragColor.rgb *= 0.5 + abs(normal.z) * 0.5;               \n"
			+ "}                                                              \n";

	public static final String SHADER_TEXTURE_VERTEX = ""
			+ "uniform mat4 uProjectionM;                                     \n"
			+ "attribute vec3 aPosition;                                      \n"
			+ "attribute vec3 aNormal;                                        \n"
			+ "attribute vec2 aTexCoord;                                      \n"
			+ "varying vec3 vNormal;                                          \n"
			+ "varying vec2 vTexCoord;                                        \n"
			+ "void main() {                                                  \n"
			+ "  gl_Position = uProjectionM * vec4(aPosition, 1.0);           \n"
			+ "  vNormal = aNormal;                                           \n"
			+ "  vTexCoord = aTexCoord;                                       \n"
			+ "}                                                              \n";

}
