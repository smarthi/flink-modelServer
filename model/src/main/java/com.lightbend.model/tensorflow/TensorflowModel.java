/*
 * Copyright (C) 2017  Lightbend
 *
 * This file is part of flink-ModelServing
 *
 * flink-ModelServing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.lightbend.model.tensorflow;

/**
 * Created by boris on 5/26/17.
 */

import com.lightbend.model.Model;
import com.lightbend.model.Modeldescriptor;
import com.lightbend.model.Winerecord;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

public class TensorflowModel implements Model {
    private Graph graph = new Graph();
    private Session session;

    public TensorflowModel(byte[] inputStream) {
        graph.importGraphDef(inputStream);
        session = new Session(graph);
    }

    @Override
    public Object score(Object input) {
        Winerecord.WineRecord record = (Winerecord.WineRecord) input;
        float[][] data = {{
                (float)record.getFixedAcidity(),
                (float)record.getVolatileAcidity(),
                (float)record.getCitricAcid(),
                (float)record.getResidualSugar(),
                (float)record.getChlorides(),
                (float)record.getFreeSulfurDioxide(),
                (float)record.getTotalSulfurDioxide(),
                (float)record.getDensity(),
                (float)record.getPH(),
                (float)record.getSulphates(),
                (float)record.getAlcohol()
        }};
        Tensor modelInput = Tensor.create(data);
        Tensor result = session.runner().feed("dense_1_input", modelInput).fetch("dense_3/Sigmoid").run().get(0);
        long[] rshape = result.shape();
        float[][] rMatrix = new float[(int)rshape[0]][(int)rshape[1]];
        result.copyTo(rMatrix);
        Intermediate value = new Intermediate(0, rMatrix[0][0]);
        for(int i=1; i < rshape[1]; i++){
            if(rMatrix[0][i] > value.getValue()) {
                value.setIndex(i);
                value.setValue(rMatrix[0][i]);
            }
        }
        return (double)value.getIndex();
    }

    @Override
    public void cleanup() {
        session.close();
        graph.close();
    }

    @Override
    public byte[] getBytes() {
        return graph.toGraphDef();
    }

    public Graph getGraph() {
        return graph;
    }

    private class Intermediate{
        private int index;
        private float value;
        public Intermediate(int i, float v){
            index = i;
            value = v;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public float getValue() {
            return value;
        }

        public void setValue(float value) {
            this.value = value;
        }
    }

    @Override
    public long getType() {
        return (long) Modeldescriptor.ModelDescriptor.ModelType.TENSORFLOW.getNumber();
    }
}
