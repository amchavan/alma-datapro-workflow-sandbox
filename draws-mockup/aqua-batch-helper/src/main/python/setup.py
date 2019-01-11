from setuptools import setup

dependencies = [
    "draws-mock-icd",
    "drawsmb"
]

setup(name='draws-mock-aqua-batch-helper',
      version='0.1',
      description='DRAWS Mock Aqua Batch Helper',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['draws', 'draws/mock', 'draws/mock/aqua'],
      install_requires=dependencies,
      zip_safe=False)
