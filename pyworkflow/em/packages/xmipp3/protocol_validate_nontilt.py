# **************************************************************************
# *
# * Authors:     Javier Vargas (jvargas@cnb.csic.es)
# *
# * Unidad de  Bioinformatica of Centro Nacional de Biotecnologia , CSIC
# *
# * This program is free software; you can redistribute it and/or modify
# * it under the terms of the GNU General Public License as published by
# * the Free Software Foundation; either version 2 of the License, or
# * (at your option) any later version.
# *
# * This program is distributed in the hope that it will be useful,
# * but WITHOUT ANY WARRANTY; without even the implied warranty of
# * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# * GNU General Public License for more details.
# *
# * You should have received a copy of the GNU General Public License
# * along with this program; if not, write to the Free Software
# * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
# * 02111-1307  USA
# *
# *  All comments concerning this program package may be sent to the
# *  e-mail address 'jmdelarosa@cnb.csic.es'
# *
# **************************************************************************

from pyworkflow.object import Float
from pyworkflow.protocol.params import (PointerParam, FloatParam,  
                                        StringParam, BooleanParam, LEVEL_ADVANCED)
from pyworkflow.em.data import Volume, SetOfParticles, SetOfVolumes
from pyworkflow.em.protocol import ProtAnalysis3D
from pyworkflow.utils.path import copyFile
from pyworkflow.em.packages.xmipp3.convert import (writeSetOfParticles,
                                                   writeSetOfVolumes)
import xmipp


class XmippProtValidateNonTilt(ProtAnalysis3D):
    """    
    Reconstruct a volume using Xmipp_reconstruct_fourier from a given set of particles.
    The alignment parameters will be converted to a Xmipp xmd file
    and used as direction projections to reconstruct.
    """
    _label = 'validate_nontilt'
            
    #--------------------------- DEFINE param functions --------------------------------------------   
    def _defineParams(self, form):
        form.addSection(label='Input')

        form.addParam('inputVolume', PointerParam, pointerClass='Volume',
                      label="Input volume",  
                      help='Select the input volume.')     

        form.addParam('inputParticles', PointerParam, pointerClass='SetOfParticles, SetOfClasses2D', 
                      label="Input particles",  
                      help='Select the input projection images .') 
            
        form.addParam('symmetryGroup', StringParam, default='c1',
                      label="Symmetry group", 
                      help='See [[Xmipp Symmetry][http://www2.mrc-lmb.cam.ac.uk/Xmipp/index.php/Conventions_%26_File_formats#Symmetry]] page '
                           'for a description of the symmetry format accepted by Xmipp') 
        
        form.addParam('angularSampling', FloatParam, default=10,
                      label="Angular Sampling (degrees)",  
                      help='Angular distance (in degrees) between neighboring projection points ')

        form.addParam('alpha', FloatParam, default=0.05,
                      label="Significance value",  
                      help='Parameter to define the corresponding most similar volume \n' 
                      '    projected images for each projection image')
        
        #form.addParallelSection(threads=1, mpi=4)

    #--------------------------- INSERT steps functions --------------------------------------------

    def _insertAllSteps(self):
        self._insertFunctionStep('convertInputStep', 
                                 self.inputParticles.get().getObjId(),
                                 self.inputVolume.get().getObjId())
        params = self._getValidateParams()
        self._insertFunctionStep('validateStep', params)
        self._insertFunctionStep('createOutputStep')
        
    def convertInputStep(self, particlesId, volumesId):
        """ Write the input images as a Xmipp metadata file. """
        writeSetOfParticles(self.inputParticles.get(), 
                            self._getTmpPath('input_particles.xmd'))
        volumes = self._createSetOfVolumes(suffix='tmp')
        volumes.append(self.inputVolume.get())
        writeSetOfVolumes(volumes,
                          self._getTmpPath('input_volume.xmd'))
            
    def _getValidateParams(self):
        params =  '  -i %s' % self._getTmpPath('input_particles.xmd')
        params += '  --volume %s' % self._getTmpPath('input_volume.xmd')
        params += ' --odir %s' % self._getTmpPath()
        params += ' --sym %s' % self.symmetryGroup.get()
        params += ' --alpha0 %0.3f' % self.alpha.get()
        params += ' --angularSampling %0.3f' % self.angularSampling.get()
        return params
    
    def validateStep(self, params):    
        self.runJob('xmipp_validation_nontilt', 
                    params)
        
    def createOutputStep(self):
        volume = self.inputVolume.get().clone()
        copyFile(self._getTmpPath('validation.xmd'), self._getExtraPath('validation.xmd'))
        copyFile(self._getTmpPath('clusteringTendency.xmd'), self._getExtraPath('clusteringTendency.xmd'))
        md = xmipp.MetaData(self._getExtraPath('validation.xmd'))
        weight = md.getValue(xmipp.MDL_WEIGHT, md.firstObject())
        volume.weight = Float(weight)                
        self._defineOutputs(outputVolume=volume)
        self._defineTransformRelation(self.inputVolume.get(), volume)
        
    #--------------------------- INFO functions -------------------------------------------- 
    def _validate(self):
        validateMsgs = []
        # if there are Volume references, it cannot be empty.
        if self.inputVolume.get() and not self.inputVolume.hasValue():
            validateMsgs.append('Please provide an input reference volume.')
        if self.inputParticles.get() and not self.inputParticles.hasValue():
            validateMsgs.append('Please provide input particles.')            
        return validateMsgs
        
    
    def _summary(self):
        summary = []
        summary.append("Input particles:  %s" % self.inputParticles.get().getNameId())
        
        if self.inputVolume.get():
            summary.append("Input volume(s): [%s]" % self.inputVolume.get())
            
        if self.outputVolume.get():
            summary.append("Output volumes not ready yet.")
        else:
            md = xmipp.MetaData(self._getExtraPath('validation.xmd'))
            weight = md.getValue(xmipp.MDL_WEIGHT, md.firstObject())
            summary.append("Output volumes: %s" % self.outputVolume.getNameId())
            summary.append("Quality parameter : %f" %  weight)
        
        return summary
    
    def _methods(self):
        messages = []
        if not self.outputVolume.get():
            md = xmipp.MetaData(self._getExtraPath('validation.xmd'))
            weight = md.getValue(xmipp.MDL_WEIGHT, md.firstObject())
            messages.append('\n')
            messages.append('We obtained the volume quality parameter of volume %s' % self.inputVolume.get().getNameId())
            messages.append('taking projections %s' % self.inputParticles.get().getNameId())
            messages.append('The obtained volume quality parameter is %f' % weight)
        return messages
    
    #--------------------------- UTILS functions --------------------------------------------
            
